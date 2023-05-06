package me.cpele.workitems.shell

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.cpele.workitems.core.Platform
import me.cpele.workitems.core.Slack

class MockSlack(platform: Platform, ingress: Ingress) : Slack {

    override val authUrlStr: String = "https://google.fr"

    override suspend fun fetchMessages(): Result<List<Slack.Message>> {
        TODO("Not yet implemented")
    }

    override suspend fun requestAuthScopes(): Flow<Slack.AuthStatus> = flow {
        emit(Slack.AuthStatus.Route.Init)
        delay(1000)
        emit(Slack.AuthStatus.Route.Started)
        delay(1000)
        val server = embeddedServer(Netty) {
            routing {
                get("/fake-code-ack") {
                    call.parameters["code"]?.let { code ->
                        call.respondText(status = HttpStatusCode.OK, text = "(Fake) Got code: $code")
                    } ?: run {
                        call.respondText(status = HttpStatusCode.BadRequest, text = "(Fake) Didn't get any code")
                    }
                }
            }
        }
        server.start()
    }

    override suspend fun tearDownLogin() {
        // Nothing to tear down
    }
}
