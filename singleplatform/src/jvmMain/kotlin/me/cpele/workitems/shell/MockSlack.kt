package me.cpele.workitems.shell

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.logging.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import me.cpele.workitems.core.Slack
import java.net.URL
import kotlin.time.Duration.Companion.seconds

object MockSlack : Slack {

    override val authUrlStr: String = "http://localhost:8080/fake-auth-url"

    private var server: ApplicationEngine? = null

    init {
        // Fake-user-image server
        GlobalScope.launch(Dispatchers.IO) {
            embeddedServer(Netty) {
                routing {
                    route("/fake-image-fake-access-token.png") {
                        get {
                            val bytes = URL("classpath:/fake-image-fake-access-token.png").readBytes()
                            call.respondBytes(bytes, ContentType.Image.PNG)
                        }
                    }
                }
            }.start(wait = true)
        }
    }

    override suspend fun fetchMessages(): Result<List<Slack.Message>> {
        TODO("Not yet implemented")
    }

    override suspend fun requestAuthScopes(): Flow<Slack.AuthenticationScopeStatus> = callbackFlow {
        send(Slack.AuthenticationScopeStatus.Route.Init)
        server = embeddedServer(Netty, host = "localhost", port = 8080) {
            routing {
                routingCodeAck(
                    callbackRoutePath = "/fake-code-ack",
                    onCode = { send(Slack.AuthenticationScopeStatus.Success(it)) },
                    onFailure = { send(Slack.AuthenticationScopeStatus.Failure(it)) })
                routingAuth("/fake-auth-url")
            }
        }
        server?.start()
        send(Slack.AuthenticationScopeStatus.Route.Started)
        server?.environment?.config?.let { serverConfig ->
            val url = URL("http", "localhost", serverConfig.port, "/fake-code-ack")
            send(Slack.AuthenticationScopeStatus.Route.Exposed(url))
        }
        awaitClose {
            launch { tearDownLogin() }
        }
    }

    private fun Routing.routingAuth(routePath: String) {
        get(routePath) {
            call.respondText(
                ContentType.Text.Html,
                HttpStatusCode.OK
            ) {
                provideSuccessfulAuthResponseText(call.request)
            }
        }
    }

    private fun Routing.routingCodeAck(
        onCode: suspend (String) -> Unit,
        onFailure: suspend (Throwable) -> Unit,
        callbackRoutePath: String
    ) {
        get(callbackRoutePath) {
            call.parameters["code"]?.let { code ->
                onCode(code)
                call.respondText(ContentType.Text.Html, HttpStatusCode.OK) {
                    provideSuccessfulCodeResponseText(code)
                }
            } ?: run {
                val msg = "(Fake) Missing `code` parameter in callback request"
                val failure = IllegalStateException(msg)
                onFailure(failure)
                call.respondText(status = HttpStatusCode.BadRequest, text = msg)
            }
        }
    }

    override suspend fun exchangeCodeForToken(code: String) = Result.success("fake-access-token")

    override suspend fun retrieveUser(accessToken: String): Result<Slack.UserInfo> = Result.success(
        Slack.UserInfo(
            id = "fake-id-$accessToken",
            name = "fake-name-$accessToken",
            presence = "fake-presence-$accessToken",
            realName = "fake-real-name-$accessToken",
            email = "fake-email-$accessToken",
            image = "https://fake-image-$accessToken.png"
        ).also {
            delay(10.seconds)
        }
    )

    override suspend fun tearDownLogin() {
        server?.stop()
        server = null
    }
}

private fun provideSuccessfulCodeResponseText(code: String) =
    buildString {
        appendHTML().html {
            body {
                style = "margin: 16px"
                p {
                    +"Got code:"
                }
                pre {
                    +code
                }
            }
        }
    }

private fun provideSuccessfulAuthResponseText(request: ApplicationRequest) =
    buildString {
        appendHTML().html {
            body {
                style = "margin: 16px"
                p { +"This is a mock of the Slack authentication service." }
                val redirectUriParamName = "redirect_uri"
                val redirectUri = request.queryParameters[redirectUriParamName]
                if (redirectUri != null) {
                    p {
                        +"Click following link to get back to app: "
                        a {
                            href = "$redirectUri?code=fake-authorization-code"
                            +href
                        }
                    }
                } else {
                    p {
                        +"Wrong request. Please check $redirectUriParamName param."
                    }
                }
                p {
                    +"Got this request:"
                    pre {
                        +request.toLogString()
                    }
                }
                p {
                    +"With headers:"
                }
                ul {
                    request.headers.toMap().forEach { header ->
                        li {
                            +"${header.key}: ${header.value.firstOrNull()}"
                        }
                    }
                }
                p {
                    +"With parameters:"
                    ul {
                        request.queryParameters.toMap().forEach { param ->
                            li {
                                +"${param.key}: ${param.value.firstOrNull()}"
                            }
                        }
                    }
                }
            }
        }
    }
