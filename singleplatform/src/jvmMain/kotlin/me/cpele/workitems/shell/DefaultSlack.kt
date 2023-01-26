package me.cpele.workitems.shell

import me.cpele.workitems.core.Slack

object DefaultSlack : Slack {
    override fun fetchMessages(): List<Slack.Message> {
        TODO("Not yet implemented")
    }
}
