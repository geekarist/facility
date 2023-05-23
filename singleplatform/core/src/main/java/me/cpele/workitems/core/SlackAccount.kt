package me.cpele.workitems.core

import oolong.Dispatch
import oolong.effect
import oolong.effect.none

object SlackAccount {

    fun init() = Change<Model, _>(Model.Blank, none<Event>())

    fun makeUpdate(
        slack: Slack, platform: Platform
    ): (Event, Model) -> Change<Model, Event> = { event, model ->
        when (event) {
            Event.SignInRequested -> Change(
                Model.Pending,
                effect {
                    platform.logi { "Got $event" }
                    slack.requestAuthScopes().collect { status ->
                        platform.logi { "Got status $status" }
                    }
                })
        }
    }

    fun view(model: Model, dispatch: Dispatch<Event>): Props = when (model) {

        is Model.Blank -> Props.SignedOut(
            title = Prop.Text(text = "Yo"),
            desc = Prop.Text(text = "Yo"),
            button = Prop.Button(text = "Sign into Slack", isEnabled = true) {
                dispatch(Event.SignInRequested)
            })

        Model.Invalid -> TODO()

        Model.Pending -> Props.SigningIn(
            title = Prop.Text("Welcome to Slaccount"),
            status = Prop.Text("Waiting for sign-in..."),
            progress = Prop.Progress(value = Math.random())
        )

        Model.Authorized -> Props.SignedIn(
            image = Prop.Image("https://TODO"),
            name = Prop.Text("Firstname lastname"),
            availability = Prop.Text("Active")
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

    sealed class Event {
        object SignInRequested : Event()
    }

    sealed interface Props {
        data class SignedOut(val title: Prop.Text, val desc: Prop.Text, val button: Prop.Button) : Props

        data class SigningIn(val title: Prop.Text, val status: Prop.Text, val progress: Prop.Progress) : Props

        data class SignedIn(val image: Prop.Image, val name: Prop.Text, val availability: Prop.Text) : Props
    }
}