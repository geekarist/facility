package me.cpele.workitems.shell

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.search.SearchMessagesRequest
import com.slack.api.methods.response.search.SearchMessagesResponse
import com.slack.api.methods.response.users.UsersGetPresenceResponse
import com.slack.api.methods.response.users.UsersInfoResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.logging.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import me.cpele.workitems.core.framework.Platform
import me.cpele.workitems.core.framework.Slack
import java.net.URL
import java.net.URLDecoder
import com.slack.api.Slack as RemoteSlack

class DefaultSlack(private val platform: Platform, private val ingress: Ingress) : Slack {

    override val authUrlStr: String = "https://slack.com/oauth/v2/authorize"

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

    override suspend fun requestAuthScopes(): Flow<Slack.AuthenticationScopeStatus> = callbackFlow {
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
                                send(Slack.AuthenticationScopeStatus.Success(code))
                            }
                            ?: run {
                                val throwable = IllegalStateException("Called without a code")
                                call.respondText(status = HttpStatusCode.BadRequest, text = throwable.toString())
                                send(Slack.AuthenticationScopeStatus.Failure(throwable))
                            }
                    } catch (throwable: Throwable) {
                        val logString = call.request.toLogString()
                        val exception = IllegalStateException("Error processing request: $logString", throwable)
                        val failure = Slack.AuthenticationScopeStatus.Failure(exception)
                        call.respondText(status = HttpStatusCode.InternalServerError, text = exception.toString())
                        send(failure)
                    }
                }
            }
        }
        server?.start()
        send(Slack.AuthenticationScopeStatus.Route.Started)
        ingress.open("http", "8080") { serverTunnel ->
            launch {
                val url = URL(serverTunnel.url, "/code-ack?tunnel=${serverTunnel.url.host}")
                platform.logi { "Server tunnel opened at URL: $url" }
                val staticUrl = wrap(url)
                send(Slack.AuthenticationScopeStatus.Route.Exposed(staticUrl))
                tunnel = serverTunnel
            }
        }
        awaitClose {
            server?.stop()
            ingress.close(tunnel)
        }
    }.catch { throwable ->
        emit(Slack.AuthenticationScopeStatus.Failure(IllegalStateException(throwable)))
    }

    @Deprecated(
        "Does not provide a user token but a bot token",
        replaceWith = ReplaceWith("exchangeCodeForCredentials(code, clientId, clientSecret, redirectUri).map { it.userToken }")
    )
    override suspend fun exchangeCodeForToken(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ) = Result.runCatching { RemoteSlack.getInstance() }.mapCatching { instance ->
        instance.methods().oauthV2Access { builder ->
            builder.clientId(clientId)
            builder.clientSecret(clientSecret)
            builder.code(code)
            builder.redirectUri(URLDecoder.decode(redirectUri))
        }
    }.mapCatching { response ->
        if (response.isOk) {
            response.accessToken
        } else {
            DesktopPlatform.logi {
                """
                    |Error exchanging code for token with request:
                    |- Code: $code
                    |- Client ID: $clientId
                    |- Client secret: $clientSecret
                    |- Redirect URI: $redirectUri
                    """.trimMargin()
            }
            error("Got error ${response.error} in response: $response")
        }
    }

    override suspend fun exchangeCodeForCredentials(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): Result<Slack.Credentials> = Result.runCatching { RemoteSlack.getInstance() }.mapCatching { instance ->
        instance.methods().oauthV2Access { builder ->
            builder.clientId(clientId)
            builder.clientSecret(clientSecret)
            builder.code(code)
            builder.redirectUri(URLDecoder.decode(redirectUri))
        }
    }.mapCatching { response ->
        if (response.isOk) {
            Slack.Credentials(response.accessToken, response.authedUser.accessToken, response.authedUser.id)
        } else {
            DesktopPlatform.logi {
                """
                    |Error exchanging code for token with request:
                    |- Code: $code
                    |- Client ID: $clientId
                    |- Client secret: $clientSecret
                    |- Redirect URI: $redirectUri
                    """.trimMargin()
            }
            error("Got error ${response.error} in response: $response")
        }
    }


    private fun wrap(url: URL): URL {
        val authority = "aloe-vera.cpele.me"
        val file = "code-ack"
        return URL("https://$authority/$file?tunnel=${url.host}")
    }

    private fun Slack.UserInfo.Companion.of(
        response: UsersInfoResponse,
        presenceResponse: UsersGetPresenceResponse
    ): Slack.UserInfo = run {
        if (presenceResponse.isOk) {
            presenceResponse.presence
        } else {
            error("Got error: ${presenceResponse.error} in response: $presenceResponse")
        }
    }.let { presenceRespStr ->
        if (response.isOk) {
            Slack.UserInfo(
                id = response.user?.id ?: error("Missing ID in user: ${response.user}"),
                name = response.user?.name ?: error("Missing user name: ${response.user}"),
                presence = presenceRespStr,
                realName = response.user?.realName ?: error("Missing user real name: ${response.user}"),
                email = response.user?.profile?.email ?: error("Missing user email: ${response.user}"),
                image = response.user?.profile?.imageOriginal ?: error("Missing user image: ${response.user}"),
            )
        } else {
            error("Got error: ${response.error} in response: $response")
        }
    }

    override suspend fun retrieveUser(credentials: Slack.Credentials): Result<Slack.UserInfo> =
        Result.runCatching {
            RemoteSlack.getInstance()
        }.mapCatching { slackInstance ->
            coroutineScope {
                val deferredUserInfo = async {
                    slackInstance.methods().usersInfo { builder ->
                        builder.token(credentials.userToken)
                        builder.user(credentials.userId)
                    }
                }
                val deferredPresence = async {
                    slackInstance.methods().usersGetPresence { builder ->
                        builder.token(credentials.userToken)
                        builder.user(credentials.userId)
                    }
                }
                val userInfo = deferredUserInfo.await()
                val presence = deferredPresence.await()
                userInfo to presence
            }
        }.mapCatching { (infoResponse, presenceResponse) ->
            Slack.UserInfo.of(infoResponse, presenceResponse)
        }


    override suspend fun revoke(accessToken: String) = Result.runCatching {
        RemoteSlack.getInstance().methods().authRevoke { it.token(accessToken) }
        Unit
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