package me.cpele.workitems.core

interface Slack {
    fun fetchMessages(): Result<List<Message>>

    interface Message {
        val text: String
    }
}