package me.cpele.workitems.core

import kotlinx.coroutines.flow.Flow

interface Slack {
    suspend fun fetchMessages(): Result<List<Message>>
    suspend fun setUpLogIn(): Flow<LoginPrepStatus>

    interface Message {
        val text: String
    }

    sealed interface LoginPrepStatus {
        sealed interface Route : LoginPrepStatus {
            object Started : Route
            object Exposed : Route
        }

        data class Success(val token: String) : LoginPrepStatus
        data class Failure(val throwable: Throwable) : LoginPrepStatus
    }
}