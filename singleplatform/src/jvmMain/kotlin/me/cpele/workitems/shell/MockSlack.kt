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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import me.cpele.workitems.core.framework.Slack
import java.net.URL
import kotlin.time.Duration.Companion.seconds

object MockSlack : Slack {

    private const val AUTH_SERVER_PORT = 8080
    private const val IMG_SERVER_PORT = 8081

    override val authUrlStr: String = "http://localhost:$AUTH_SERVER_PORT/fake-auth-url"

    private var server: ApplicationEngine? = null

    init {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launchFakeUserImageServer()
    }

    private fun CoroutineScope.launchFakeUserImageServer() {
        launch(Dispatchers.IO) {
            embeddedServer(factory = Netty, port = IMG_SERVER_PORT) {
                routing {
                    route("/fake-image/{imgName}", HttpMethod.Get) {
                        handle {
                            val imgName = call.parameters["imgName"]
                            DesktopPlatform.logi { "Got img path: $imgName" }
                            val pkg = MockSlack::class.java.`package`.name
                            val pkgPath = pkg.replace('.', '/')
                            val path = "$pkgPath/fake-image/$imgName"
                            try {
                                val resource = javaClass.classLoader.getResource(path)
                                val bytes = resource?.readBytes()
                                    ?: throw IllegalStateException("Resource not found at path: $path")
                                DesktopPlatform.logi { "File read successfully ⇒ Returning image" }
                                call.respondBytes(bytes, ContentType.Image.PNG)
                            } catch (t: Throwable) {
                                DesktopPlatform.logi(t) { "Error reading file from path: $path" }
                                call.respondText(
                                    status = HttpStatusCode.InternalServerError,
                                    text = "Error loading image"
                                )
                            }
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
        server = embeddedServer(Netty, host = "localhost", port = AUTH_SERVER_PORT) {
            routing {
                setUpCodeAckRoute(
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

    private fun Routing.setUpCodeAckRoute(
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

    override suspend fun revoke(accessToken: String) = Result.success(Unit)

    @Deprecated(
        "Does not provide a user token but a bot token",
        replaceWith = ReplaceWith("exchangeCodeForCredentials(code, clientId, clientSecret, redirectUri).map { it.userToken }")
    )
    override suspend fun exchangeCodeForToken(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): Result<String> =
        Result.success("fake-access-token")

    override suspend fun exchangeCodeForCredentials(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): Result<Slack.Credentials> = Result.runCatching {
        Slack.Credentials(
            botToken = "fake-bot-token-$code",
            userToken = "fake-user-token-$code",
            userId = "fake-user-id-$code"
        )
    }

    override suspend fun retrieveUser(credentials: Slack.Credentials): Result<Slack.UserInfo> =
        Result.runCatching {
            val accessToken = credentials.userToken
            Slack.UserInfo(
                id = "fake-id-$accessToken",
                name = "fake-name-$accessToken",
                presence = "fake-presence-$accessToken",
                realName = "fake-real-name-$accessToken",
                email = "fake-email-$accessToken",
                image = "http://localhost:${IMG_SERVER_PORT}/fake-image/$accessToken.png"
            ).also {
                delay(10.seconds)
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
