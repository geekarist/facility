package me.cpele.workitems.shell

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.search.SearchMessagesRequest
import com.slack.api.methods.response.search.SearchMessagesResponse
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import me.cpele.workitems.core.Slack
import java.awt.Desktop
import java.net.URI
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
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

    override suspend fun logIn(): Result<String> = Result.runCatching {
        withContext(Dispatchers.IO) {
            val clientId = System.getProperty("slack.client.id")
            val userScope = "search:read"
            val baseUrl = "https://slack.com/oauth/v2/authorize"
            val redirectUri = "https://TODO:TODO/auth-callback-ack"
            val uuid = UUID.randomUUID().toString()
            val tmpAuthorizationCode = suspendCoroutine { continuation: Continuation<String> ->
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.launch {
                    embeddedServer(factory = Netty, port = 8080) {
                        routing {
                            get("/auth-callback-ack") {
                                val params = call.request.queryParameters
                                val code = params["code"]
                                val state = params["state"]
                                if (state == uuid && code != null) {
                                    continuation.resume(code)
                                } else if (state != uuid) {
                                    continuation.resumeWithException(
                                        IllegalStateException("Called back with invalid state: $state. Should be: $uuid")
                                    )
                                } else {
                                    continuation.resumeWithException(
                                        IllegalStateException("Called back with null in params: $params")
                                    )
                                }
                            }
                        }
                    }.start(wait = true)
                }
                val url = "$baseUrl?client_id=$clientId&user_scope=$userScope&redirect_uri=$redirectUri&state=$uuid"
                Desktop.getDesktop().browse(URI.create(url))
            }
            Logger.getAnonymousLogger()
                .log(Level.FINE, "Got temporary authorization code: $tmpAuthorizationCode")
            TODO()
        }
    }

    data class Message(override val text: String) : Slack.Message
}

