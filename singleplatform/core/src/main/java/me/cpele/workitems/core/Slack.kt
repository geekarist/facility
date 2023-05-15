package me.cpele.workitems.core

import kotlinx.coroutines.flow.Flow
import java.net.URL

interface Slack {

    val authUrlStr: String

    suspend fun fetchMessages(): Result<List<Message>>
    suspend fun requestAuthScopes(): Flow<AuthenticationStatus>
    suspend fun tearDownLogin()
    suspend fun exchangeCodeForToken(code: String): Result<String>

    interface Message {
        val text: String
    }

    sealed interface AuthenticationStatus {
        sealed interface Route : AuthenticationStatus {
            object Init : Route
            object Started : Route
            data class Exposed(val url: URL) : Route
        }

        data class Success(val code: String) : AuthenticationStatus
        data class Failure(val throwable: Throwable) : AuthenticationStatus
    }
}