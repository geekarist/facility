package me.cpele.workitems.core

import kotlinx.coroutines.flow.Flow

interface Slack {
    suspend fun fetchMessages(): Result<List<Message>>
    suspend fun setUpLogIn(): Flow<LoginStatus>

    interface Message {
        val text: String
    }

    sealed interface LoginStatus {
        sealed interface Route : LoginStatus {
            object Started : Route
            object Exposed : Route
        }

        data class Success(val token: String) : LoginStatus
        data class Failure(val throwable: Throwable) : LoginStatus
    }
}