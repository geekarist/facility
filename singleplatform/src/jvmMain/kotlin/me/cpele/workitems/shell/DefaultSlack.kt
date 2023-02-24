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
import java.io.File
import java.net.URI
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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
            // Start local HTTP route
            val localPort = 8080
            val uuid = UUID.randomUUID().toString()
            val exposedPath = "/auth-callback-ack"
            val deferredTmpAuthCode = asyncExpectCodeHttpCallback(port = localPort, path = exposedPath, uuid = uuid)

            // Expose local HTTP route, get exposed URL
            val exposeLogFile = File.createTempFile(uuid, ".json")
            val exposeLogPath = exposeLogFile.path
            val exposeProcess = asyncExposeLocalHttpServer(port = localPort, path = exposeLogPath)
            val exposedBaseUrl = extractExposedBaseUrl(exposeLogPath)
            exposeProcess?.destroy()
            exposeLogFile.delete()

            // Build authorization URL, open with browser
            val exposedUrl = "$exposedBaseUrl:$localPort/$exposedPath"
            val authUrl = authUrlOf(System.getProperty("slack.client.id"), exposedUrl, uuid)
            Desktop.getDesktop().browse(URI.create(authUrl))

            // Await temporary auth code
            val tmpAuthorizationCode = deferredTmpAuthCode.await()

            // TODO: Exchange code for token
            Logger.getAnonymousLogger()
                .log(Level.FINE, "Got temporary authorization code: $tmpAuthorizationCode")
            TODO()
        }
    }

    private fun asyncExposeLocalHttpServer(port: Int, path: String): Process? {
        val command = "ngrok http $port --log-format=json --log=$path"
        return Runtime.getRuntime().exec(command) ?: throw IllegalStateException("Got")
    }

    private fun authUrlOf(clientId: String?, exposedUrl: String, uuid: String): String {
        val baseUrl = "https://slack.com/oauth/v2/authorize"
        val userScope = "search:read"
        return "$baseUrl?client_id=$clientId&user_scope=$userScope&redirect_uri=$exposedUrl&state=$uuid"
    }

    private fun extractExposedBaseUrl(exposeLogPath: String): String {
        TODO("Not yet implemented")
    }

    private suspend fun asyncExpectCodeHttpCallback(port: Int, path: String, uuid: String): Deferred<String> =
        coroutineScope {
            async {
                suspendCancellableCoroutine { continuation ->
                    val server = embeddedServer(factory = Netty, port = port) {
                        routing {
                            get(path) {
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
                    }
                    continuation.invokeOnCancellation {
                        server.stop()
                    }
                    server.start(wait = true)
                }
            }
        }

    data class Message(override val text: String) : Slack.Message
}

