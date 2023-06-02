package me.cpele.workitems.core

import oolong.Dispatch
import oolong.effect.none

object SlackAccount {

    fun init() = Change<Model, _>(Model.Blank, none<Event>())

    fun makeUpdate(
        slack: Slack, platform: Platform
    ): (Event, Model) -> Change<Model, Event> = { event, model ->
        when (event) {

            Event.Intent.SignIn -> Change(Model.Pending) { dispatch ->
                platform.logi { "Got $event" }
                slack.requestAuthScopes().collect { status ->
                    platform.logi { "Got status $status" }
                    dispatch(Event.Outcome.AuthScopeStatus(status))
                }
            }

            Event.Intent.SignInCancel -> Change(Model.Blank) {
                slack.tearDownLogin()
            }

            is Event.Outcome.AuthScopeStatus -> handleAuthScopeStatusOutcome(event, slack, platform)

            Event.Intent.SignOut -> Change(model) {
                platform.logi { "TODO: Handle $event" }
            }

            is Event.Outcome.AccessToken -> Change(model) {
                platform.logi { "\uD83D\uDE0C Got auth token: $event" }
            }
        }
    }

    private fun handleAuthScopeStatusOutcome(
        event: Event.Outcome.AuthScopeStatus,
        slack: Slack,
        platform: Platform
    ): Change<Model, Event> = when (event.status) {
        is Slack.AuthenticationScopeStatus.Failure -> Change(Model.Invalid) {
            slack.tearDownLogin()
        }

        is Slack.AuthenticationScopeStatus.Route.Exposed -> Change(Model.Pending) {
            platform.logi {
                "Callback server exposed. " +
                        "A fake authorization code can be sent through this URL: " +
                        "${event.status.url}?code=fake-auth-code"
            }
        }

        Slack.AuthenticationScopeStatus.Route.Init,
        Slack.AuthenticationScopeStatus.Route.Started -> Change(Model.Pending)

        is Slack.AuthenticationScopeStatus.Success -> Change(Model.Authorized) { dispatch ->
            val authorizationCode = event.status.code
            val tokenResult = slack.exchangeCodeForToken(authorizationCode)
            dispatch(Event.Outcome.AccessToken(tokenResult))
        }
    }

    fun view(model: Model, dispatch: Dispatch<Event>): Props = when (model) {

        is Model.Blank -> Props.SignedOut(
            title = Prop.Text(text = "Welcome to Slaccount"),
            desc = Prop.Text(text = "Please sign in with your Slack account to display your personal info"),
            button = Prop.Button(text = "Sign into Slack", isEnabled = true) {
                dispatch(Event.Intent.SignIn)
            })

        Model.Invalid -> TODO()

        Model.Pending -> Props.SigningIn(
            title = Prop.Text("Welcome to Slaccount"),
            progress = Prop.Progress(value = Math.random().toFloat()),
            cancel = Prop.Button(text = "Cancel") {
                dispatch(Event.Intent.SignInCancel)
            },
            Prop.Text("We need your permission to let Slack give us info about you."),
            Prop.Text("Waiting for you to sign into Slack through a web-browser window...")
        )

        Model.Authorized -> Props.SignedIn(
            image = null,
            name = Prop.Text("Firstname lastname"),
            availability = Prop.Text("Active"),
            signOut = Prop.Button("Sign out") { dispatch(Event.Intent.SignOut) }
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

    sealed interface Event {
        sealed interface Intent : Event {
            object SignOut : Event
            object SignIn : Event
            object SignInCancel : Event
        }

        sealed interface Outcome : Event {
            data class AuthScopeStatus(val status: Slack.AuthenticationScopeStatus) : Event
            data class AccessToken(val token: Result<String>) : Event
        }
    }

    sealed interface Props {
        data class SignedOut(val title: Prop.Text, val desc: Prop.Text, val button: Prop.Button) : Props

        data class SigningIn(
            val title: Prop.Text,
            val progress: Prop.Progress,
            val cancel: Prop.Button,
            val statuses: List<Prop.Text>
        ) : Props {
            companion object {
                operator fun invoke(
                    title: Prop.Text,
                    progress: Prop.Progress,
                    cancel: Prop.Button,
                    vararg status: Prop.Text
                ) = SigningIn(title, progress, cancel, status.asList())
            }
        }

        data class SignedIn(
            /** Account image. When absent, `null` */
            val image: Prop.Image?,
            val name: Prop.Text,
            val availability: Prop.Text,
            val signOut: Prop.Button
        ) : Props
    }
}