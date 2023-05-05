package me.cpele.workitems.shell

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.cpele.workitems.core.Platform
import me.cpele.workitems.core.Slack
import java.net.URL

class MockSlack(platform: Platform, ingress: Ingress) : Slack {
    override suspend fun fetchMessages(): Result<List<Slack.Message>> {
        TODO("Not yet implemented")
    }

    override suspend fun requestAuthScopes(): Flow<Slack.AuthStatus> = flow {
        emit(Slack.AuthStatus.Route.Init)
        delay(1000)
        emit(Slack.AuthStatus.Route.Started)
        delay(1000)
        emit(Slack.AuthStatus.Route.Exposed(URL("https://fake.cpele.me/23005018581")))
    }

    override suspend fun tearDownLogin() {
        // Nothing to tear down
    }
}
