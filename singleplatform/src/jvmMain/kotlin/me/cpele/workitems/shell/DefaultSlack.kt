package me.cpele.workitems.shell

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.search.SearchMessagesRequest
import com.slack.api.methods.response.search.SearchMessagesResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.logging.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.cpele.workitems.core.Platform
import me.cpele.workitems.core.Slack
import com.slack.api.Slack as RemoteSlack

class DefaultSlack(private val platform: Platform, private val ingress: Ingress) : Slack {

    private var server: ApplicationEngine? = null
    private var tunnel: Ingress.Tunnel? = null

    override suspend fun fetchMessages() = Result.runCatching {
        withContext(Dispatchers.IO) {
            val slack: RemoteSlack = RemoteSlack.getInstance()
            val token = System.getProperty("slack.token")
            val methods: MethodsClient = slack.methods(token)
            val request: SearchMessagesRequest = SearchMessagesRequest.builder().token(token).query("hello").build()
            val response: SearchMessagesResponse = methods.searchMessages(request)
            if (response.isOk) {
                response.messages.matches.map {
                    Message(it.text)
                }
            } else {
                throw IllegalStateException("Error fetching Slack messages, request: $request, response: $response")
            }
        }
    }

    override suspend fun setUpLogin(): Flow<Slack.LoginStatus> = callbackFlow {
        server?.stop()
        server = embeddedServer(factory = Netty, port = 8080) {
            routing {
                trace { routingTrace ->
                    val requestLogStr = routingTrace.call.request.toLogString()
                    val msg = "Got routing trace: $routingTrace, call request: $requestLogStr"
                    platform.logi { msg }
                }
                get("/code-ack") {
                    try {
                        call.parameters["token"]
                            ?.let { token ->
                                call.respondText(status = HttpStatusCode.OK, text = "Got token: $token")
                                send(Slack.LoginStatus.Success(token))
                            } ?: send(Slack.LoginStatus.Failure(IllegalStateException("Called without a token")))
                    } catch (throwable: Throwable) {
                        val logString = call.request.toLogString()
                        val exception = IllegalStateException("Error processing request: $logString", throwable)
                        val failure = Slack.LoginStatus.Failure(exception)
                        send(failure)
                    }
                }
            }
        }
        server?.start()
        send(Slack.LoginStatus.Route.Started)
        ingress.open("http", "8080") { serverTunnel ->
            launch {
                send(Slack.LoginStatus.Route.Exposed(serverTunnel.url))
                tunnel = serverTunnel
            }
        }
        awaitClose {
            server?.stop()
            ingress.close(tunnel)
        }
    }.catch { throwable ->
        emit(Slack.LoginStatus.Failure(IllegalStateException(throwable)))
    }

    override suspend fun tearDownLogin() {
        server?.stop()
        server = null
        tunnel?.let {
            ingress.close(it)
        }
        tunnel = null
    }

    data class Message(override val text: String) : Slack.Message
}

