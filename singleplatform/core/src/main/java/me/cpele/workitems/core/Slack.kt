package me.cpele.workitems.core

interface Slack {
    fun fetchMessages(): Result<List<Message>>
    fun logIn(): Result<String>

    interface Message {
        val text: String
    }
}