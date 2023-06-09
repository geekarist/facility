package me.cpele.workitems.core

import kotlinx.coroutines.flow.Flow
import java.net.URL

interface Slack {

    val authUrlStr: String

    suspend fun fetchMessages(): Result<List<Message>>
    suspend fun requestAuthScopes(): Flow<AuthenticationScopeStatus>
    suspend fun tearDownLogin()
    suspend fun exchangeCodeForToken(code: String): Result<String>
    suspend fun retrieveUser(accessToken: String): Result<UserInfo>

    interface Message {
        val text: String
    }

    interface UserInfo {
        companion object
    }

    sealed interface AuthenticationScopeStatus {

        sealed interface Route : AuthenticationScopeStatus {
            object Init : Route
            object Started : Route
            data class Exposed(val url: URL) : Route
        }

        data class Success(val code: String) : AuthenticationScopeStatus
        data class Failure(val throwable: Throwable) : AuthenticationScopeStatus
    }
}