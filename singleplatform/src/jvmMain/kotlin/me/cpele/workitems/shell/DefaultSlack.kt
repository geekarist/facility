package me.cpele.workitems.shell

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.search.SearchMessagesRequest
import com.slack.api.methods.response.search.SearchMessagesResponse
import me.cpele.workitems.core.Slack
import com.slack.api.Slack as RemoteSlack

object DefaultSlack : Slack {
    override fun fetchMessages() = Result.runCatching {
        val slack: RemoteSlack = RemoteSlack.getInstance()
        val token = System.getProperty("slack.token")
        val methods: MethodsClient = slack.methods(token)
        val request: SearchMessagesRequest = SearchMessagesRequest.builder().token(token).query("hello").build()
        val response: SearchMessagesResponse = methods.searchMessages(request)
        if (response.isOk) {
            response.messages.matches.map {
                Message(it.text)
            }
        } else {
            throw IllegalStateException("Error fetching Slack messages, request: $request, response: $response")
        }
    }

    data class Message(override val text: String) : Slack.Message
}
