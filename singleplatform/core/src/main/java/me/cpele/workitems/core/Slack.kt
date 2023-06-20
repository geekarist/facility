package me.cpele.workitems.core

import kotlinx.coroutines.flow.Flow
import java.net.URL

interface Slack {

    val authUrlStr: String

    suspend fun fetchMessages(): Result<List<Message>>
    suspend fun requestAuthScopes(): Flow<AuthenticationScopeStatus>
    suspend fun tearDownLogin()
    suspend fun exchangeCodeForToken(code: String, clientId: String, redirectUri: String): Result<String>
    suspend fun retrieveUser(accessToken: String): Result<UserInfo>
    suspend fun revoke(accessToken: String): Result<Unit>

    interface Message {
        val text: String
    }

    data class UserInfo(
        val id: String,
        val name: String,
        val presence: String,
        val realName: String,
        val email: String,
        val image: String
    ) {
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