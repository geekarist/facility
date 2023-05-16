package me.cpele.workitems.core

import oolong.Dispatch
import oolong.effect.none

object SlackAccount {

    fun init() = Change<Model, _>(Model.Blank, none<Event>())

    fun makeUpdate(
        slack: Slack,
        platform: Platform
    ): (Event, Model) -> Change<Model, Event> = { event, model ->
        when (event) {
            else -> Change(model, none())
        }
    }

    fun view(model: Model, dispatch: Dispatch<Event>) = when (model) {
        is Model.Blank -> Props(
            // "Sign in..." button, enabled
            Button(text = "Sign into Slack", isEnabled = true) {}
        )

        Model.Invalid -> Props(
            // "Retry sign-in..." button, enabled
            Button(text = "Retry Slack sign-in", isEnabled = true) {}
            // Dialog with partial account data, authentication status detail
        )

        Model.Pending -> Props(
            // "Signing in" button, disabled
            Button(text = "Signing in...", isEnabled = false) {}
            // Dialog with partial account data, authentication status detail
        )

        Model.Authorized -> Props(
            // "Sign out" button, enabled
            Button(text = "Sign out from Slack", isEnabled = false) {}
            // Dialog with account data
        )
    }

    /**
     * This model represents a Slack user account.
     */
    sealed interface Model {
        /** Authentication process wasn't even started */
        object Blank : Model

        /** Authentication started but not complete */
        object Pending : Model

        /** Authentication failed at some point */
        object Invalid : Model

        /** Authentication was successful */
        object Authorized : Model
    }

    class Event {
    }

    data class Props(val button: Button)
}