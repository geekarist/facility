package me.cpele.workitems.core

import kotlinx.coroutines.flow.Flow
import java.net.URL

interface Slack {
    suspend fun fetchMessages(): Result<List<Message>>
    suspend fun setUpLogin(): Flow<LoginStatus>
    suspend fun tearDownLogin()

    interface Message {
        val text: String
    }

    sealed interface LoginStatus {
        sealed interface Route : LoginStatus {
            object Started : Route
            data class Exposed(val url: URL) : Route
        }

        data class Success(val token: String) : LoginStatus
        data class Failure(val throwable: Throwable) : LoginStatus
    }
}