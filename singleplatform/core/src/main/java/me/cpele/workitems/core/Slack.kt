package me.cpele.workitems.core

interface Slack {
    suspend fun fetchMessages(): Result<List<Message>>
    suspend fun logIn(): Result<String>

    interface Message {
        val text: String
    }
}