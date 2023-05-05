package me.cpele.workitems.core

import kotlinx.coroutines.flow.Flow
import java.net.URL

interface Slack {

    val authUrlStr: String

    suspend fun fetchMessages(): Result<List<Message>>
    suspend fun requestAuthScopes(): Flow<AuthStatus>
    suspend fun tearDownLogin()

    interface Message {
        val text: String
    }

    sealed interface AuthStatus {
        sealed interface Route : AuthStatus {
            object Init : Route
            object Started : Route
            data class Exposed(val url: URL) : Route
        }

        data class Success(val code: String) : AuthStatus
        data class Failure(val throwable: Throwable) : AuthStatus
    }
}