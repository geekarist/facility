package me.cpele.workitems.shell

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import me.cpele.workitems.core.Platform
import me.cpele.workitems.core.Slack
import java.net.URL

class MockSlack(platform: Platform, ingress: Ingress) : Slack {

    override val authUrlStr: String = "https://google.fr"

    var server: ApplicationEngine? = null

    override suspend fun fetchMessages(): Result<List<Slack.Message>> {
        TODO("Not yet implemented")
    }

    override suspend fun requestAuthScopes(): Flow<Slack.AuthStatus> = callbackFlow {
        send(Slack.AuthStatus.Route.Init)
        val callbackRoutePath = "/fake-code-ack"
        val serverHost = "localhost"
        server = embeddedServer(Netty, host = serverHost, port = 8080) {
            routing {
                get(callbackRoutePath) {
                    call.parameters["code"]?.let { code ->
                        call.respondText(status = HttpStatusCode.OK, text = "(Fake) Got code: $code")
                    } ?: run {
                        call.respondText(
                            status = HttpStatusCode.BadRequest,
                            text = "(Fake) Missing `code` parameter in callback request"
                        )
                    }
                }
            }
        }
        server?.start()
        send(Slack.AuthStatus.Route.Started)
        server?.environment?.config?.let { serverConfig ->
            val url = URL("http", serverHost, serverConfig.port, callbackRoutePath)
            send(Slack.AuthStatus.Route.Exposed(url))
        }
        awaitClose {
            launch { tearDownLogin() }
        }
    }

    override suspend fun tearDownLogin() {
        server?.stop()
        server = null
    }
}
