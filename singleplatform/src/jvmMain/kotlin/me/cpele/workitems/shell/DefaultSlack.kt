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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import me.cpele.workitems.core.Slack
import java.util.logging.Level
import java.util.logging.Logger
import com.slack.api.Slack as RemoteSlack

object DefaultSlack : Slack {
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

    override suspend fun setUpLogIn(): Flow<Slack.LoginStatus> = flow {
        val emit: suspend (Slack.LoginStatus) -> Unit = { status ->
            withContext(Dispatchers.Default) {
                emit(status)
            }
        }
        embeddedServer(factory = Netty, port = 8080) {
            routing {
                trace {
                    logi { "Got routing trace: $it, call request: ${it.call.request.toLogString()}" }
                }
                get("/code-ack") {
                    try {
                        call.parameters["token"]
                            ?.let { token ->
                                call.respondText(status = HttpStatusCode.OK, text = "Got token: $token")
                                emit(Slack.LoginStatus.Success(token))
                            } ?: emit(Slack.LoginStatus.Failure(IllegalStateException("Called without a token")))
                    } catch (throwable: Throwable) {
                        val logString = call.request.toLogString()
                        val exception = IllegalStateException("Error processing request: $logString", throwable)
                        val failure = Slack.LoginStatus.Failure(exception)
                        emit(failure)
                    }
                }
            }
        }.start()
        emit(Slack.LoginStatus.Route.Started)
    }.catch { throwable ->
        emit(Slack.LoginStatus.Failure(IllegalStateException(throwable)))
    }

    private inline fun logi(msg: () -> String) = Logger.getAnonymousLogger().log(Level.INFO, msg())

    data class Message(override val text: String) : Slack.Message
}

