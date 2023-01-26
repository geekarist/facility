package me.cpele.workitems.shell

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.search.SearchMessagesRequest
import com.slack.api.methods.response.search.SearchMessagesResponse
import me.cpele.workitems.core.Slack
import com.slack.api.Slack as RemoteSlack

object DefaultSlack : Slack {
    override fun fetchMessages() = Result.runCatching {
        val slack: RemoteSlack = RemoteSlack.getInstance()
        val token = "TODO"
        val methods: MethodsClient = slack.methods(token)
        val request: SearchMessagesRequest = SearchMessagesRequest.builder().query("hello").build()
        val response: SearchMessagesResponse = methods.searchMessages(request)
        response.messages.matches.map {
            Message(it.text)
        }
    }

    data class Message(override val text: String) : Slack.Message
}
