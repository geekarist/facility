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
import java.net.URL
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
                        call.parameters["code"]
                            ?.let { code ->
                                call.respondText(status = HttpStatusCode.OK, text = "Got code: $code")
                                send(Slack.LoginStatus.Success(code))
                            }
                            ?: run {
                                val throwable = IllegalStateException("Called without a code")
                                call.respondText(status = HttpStatusCode.BadRequest, text = throwable.toString())
                                send(Slack.LoginStatus.Failure(throwable))
                            }
                    } catch (throwable: Throwable) {
                        val logString = call.request.toLogString()
                        val exception = IllegalStateException("Error processing request: $logString", throwable)
                        val failure = Slack.LoginStatus.Failure(exception)
                        call.respondText(status = HttpStatusCode.InternalServerError, text = exception.toString())
                        send(failure)
                    }
                }
            }
        }
        server?.start()
        send(Slack.LoginStatus.Route.Started)
        ingress.open("http", "8080") { serverTunnel ->
            launch {
                val url = URL(serverTunnel.url, "/code-ack")
                platform.logi { "Server tunnel opened at URL: $url" }
                val staticUrl = wrap(url)
                send(Slack.LoginStatus.Route.Exposed(staticUrl))
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

    private fun wrap(url: URL): URL {
        val authority = "vps-134bd385.vps.ovh.net"
        val port = 3000
        val file = "code-ack"
        return URL(url, "https://$authority:$port/$file")
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