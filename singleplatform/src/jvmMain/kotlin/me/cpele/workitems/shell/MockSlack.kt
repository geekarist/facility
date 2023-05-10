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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import me.cpele.workitems.core.Slack
import java.net.URL

object MockSlack : Slack {

    override val authUrlStr: String = "http://localhost:8080/fake-auth-url"

    var server: ApplicationEngine? = null

    override suspend fun fetchMessages(): Result<List<Slack.Message>> {
        TODO("Not yet implemented")
    }

    override suspend fun requestAuthScopes(): Flow<Slack.AuthStatus> = callbackFlow {
        send(Slack.AuthStatus.Route.Init)
        server = embeddedServer(Netty, host = "localhost", port = 8080) {
            routing {
                routingCodeAck("/fake-code-ack")
                routingAuth("/fake-auth-url")
            }
        }
        server?.start()
        send(Slack.AuthStatus.Route.Started)
        server?.environment?.config?.let { serverConfig ->
            val url = URL("http", "localhost", serverConfig.port, "/fake-code-ack")
            send(Slack.AuthStatus.Route.Exposed(url))
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

    private fun Routing.routingCodeAck(callbackRoutePath: String) {
        get(callbackRoutePath) {
            call.parameters["code"]?.let { code ->
                call.respondText(ContentType.Text.Html, HttpStatusCode.OK) {
                    provideSuccessfulCodeResponseText(code)
                }
            } ?: run {
                call.respondText(
                    status = HttpStatusCode.BadRequest,
                    text = "(Fake) Missing `code` parameter in callback request"
                )
            }
        }
    }


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
