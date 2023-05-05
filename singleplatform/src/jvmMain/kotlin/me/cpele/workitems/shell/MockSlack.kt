package me.cpele.workitems.shell

import kotlinx.coroutines.flow.Flow
import me.cpele.workitems.core.Platform
import me.cpele.workitems.core.Slack

class MockSlack(platform: Platform, ingress: Ingress) : Slack {
    override suspend fun fetchMessages(): Result<List<Slack.Message>> {
        TODO("Not yet implemented")
    }

    override suspend fun requestAuthScopes(): Flow<Slack.AuthStatus> {
        TODO("Not yet implemented")
    }

    override suspend fun tearDownLogin() {
        TODO("Not yet implemented")
    }
}
