package me.cpele.workitems.core

import oolong.Dispatch
import oolong.effect.none
object SlackAccount {

    data class Ctx(val slack: Slack, val platform: Platform)

    fun init() = Change<Model, _>(Model.Blank, none<Event>())

    fun makeUpdate(
        ctx: Ctx
    ): (Event, Model) -> Change<Model, Event> = { event, model ->
        handle(ctx, event, model)
    }

    private fun handle(
        ctx: Ctx,
        event: Event,
        model: Model
    ): Change<Model, Event> = when (event) {
        is Event.Intent.SignIn -> handle(ctx, event)
        is Event.Outcome.AuthScopeStatus -> handle(ctx, event)
        Event.Intent.SignInCancel -> Change(Model.Blank) { ctx.slack.tearDownLogin() }
        is Event.Outcome.AccessToken -> handle(ctx, event)
        Event.Intent.SignOut -> Change(model) { ctx.platform.logi { "TODO: Handle $event" } }
    }

    private fun handle(
        ctx: Ctx,
        event: Event.Outcome.AccessToken
    ): Change<Model, Event> = Change(
        event.token.fold(
            onSuccess = { Model.Authorized(it) },
            onFailure = { Model.Invalid(it) })
    ) { ctx.platform.logi { "\uD83D\uDE0C Got auth token: $event" } }

    private fun handle(
        ctx: Ctx,
        event: Event.Intent.SignIn
    ): Change<Model, Event> = Change(Model.Pending) { dispatch ->
        ctx.platform.logi { "Got $event" }
        ctx.slack.requestAuthScopes().collect { status ->
            ctx.platform.logi { "Got status $status" }
            dispatch(Event.Outcome.AuthScopeStatus(status))
        }
    }

    private fun handle(
        ctx: Ctx,
        event: Event.Outcome.AuthScopeStatus
    ): Change<Model, Event> = when (event.status) {

        is Slack.AuthenticationScopeStatus.Failure -> Change(Model.Invalid(event.status.throwable)) {
            ctx.slack.tearDownLogin()
        }

        is Slack.AuthenticationScopeStatus.Route.Exposed -> Change(Model.Pending) {
            ctx.platform.logi {
                "Callback server exposed. " +
                        "A fake authorization code can be sent through this URL: " +
                        "${event.status.url}?code=fake-auth-code"
            }
        }

        Slack.AuthenticationScopeStatus.Route.Init,
        Slack.AuthenticationScopeStatus.Route.Started -> Change(Model.Pending)

        is Slack.AuthenticationScopeStatus.Success -> Change(Model.Pending) { dispatch ->
            val authorizationCode = event.status.code
            val tokenResult = ctx.slack.exchangeCodeForToken(authorizationCode)
            dispatch(Event.Outcome.AccessToken(tokenResult))
        }
    }

    fun view(model: Model, dispatch: Dispatch<Event>): Props = when (model) {
        is Model.Blank -> view(model, dispatch)
        is Model.Invalid -> TODO()
        is Model.Pending -> view(model, dispatch)
        is Model.Authorized -> view(model, dispatch)
    }

    private fun view(
        model: Model.Authorized,
        dispatch: Dispatch<Event>
    ) = Props.SignedIn(
        image = null,
        name = Prop.Text("Firstname lastname"),
        availability = Prop.Text("Active"),
        token = Prop.Text("Access token: ${model.accessToken}"),
        signOut = Prop.Button("Sign out") { dispatch(Event.Intent.SignOut) }
    )

    private fun view(
        @Suppress("UNUSED_PARAMETER") model: Model.Pending,
        dispatch: Dispatch<Event>
    ) = Props.SigningIn(
        title = Prop.Text("Welcome to Slaccount"),
        progress = Prop.Progress(value = Math.random().toFloat()),
        cancel = Prop.Button(text = "Cancel") {
            dispatch(Event.Intent.SignInCancel)
        },
        Prop.Text("We need your permission to let Slack give us info about you."),
        Prop.Text("Waiting for you to sign into Slack through a web-browser window...")
    )

    private fun view(
        @Suppress("UNUSED_PARAMETER") model: Model.Blank,
        dispatch: Dispatch<Event>
    ) = Props.SignedOut(
        title = Prop.Text(text = "Welcome to Slaccount"),
        desc = Prop.Text(text = "Please sign in with your Slack account to display your personal info"),
        button = Prop.Button(text = "Sign into Slack", isEnabled = true) {
            dispatch(Event.Intent.SignIn)
        })

    /**
     * This model represents a Slack user account.
     */
    sealed interface Model {
        /** Authentication process wasn't even started */
        object Blank : Model

        /** Authentication started but not complete */
        object Pending : Model

        /** Authentication failed at some point */
        data class Invalid(val throwable: Throwable) : Model

        /** Authentication was successful */
        data class Authorized(val accessToken: String) : Model
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
            val token: Prop.Text,
            val signOut: Prop.Button
        ) : Props
    }
}

