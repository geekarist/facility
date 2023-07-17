package me.cpele.workitems.core.programs

import me.cpele.workitems.core.framework.*
import oolong.Dispatch
import oolong.effect.map
import oolong.effect.none
// TODO: Don't use Java URL encoder
import java.net.URLEncoder
import java.nio.charset.Charset

private const val SLACK_CLIENT_ID = "961165435895.5012210604118"

object SlackAccount {

    // region Model

    /**
     * This model represents a Slack user account.
     */
    sealed interface Model {
        /** Authentication process wasn't even started */
        object Blank : Model

        /** Authentication started but not complete */
        data class Pending(val redirectUri: String? = null) : Model

        /** Authentication failed at some point */
        data class Invalid(val throwable: Throwable) : Model

        /** Authentication was successful */
        data class Authorized(val accessToken: String) : Model

        data class Retrieved(val subModel: SlackRetrievedAccount.Model) : Model
    }

    // endregion

    // region View

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

        data class Retrieved(val subProps: SlackRetrievedAccount.Props) : Props
    }

    fun view(model: Model, dispatch: Dispatch<Event>): Props = when (model) {
        is Model.Blank -> props(model, dispatch)
        is Model.Invalid -> props(model, dispatch)
        is Model.Pending -> props(model, dispatch)
        is Model.Authorized -> props(model, dispatch)
        is Model.Retrieved -> props(model, dispatch)
    }

    private fun props(
        model: Model.Retrieved,
        dispatch: (Event) -> Unit
    ): Props = Props.Retrieved(SlackRetrievedAccount.view(model.subModel) { event ->
        dispatch(Event.Retrieved(event))
    })

    private fun props(@Suppress("UNUSED_PARAMETER") model: Model.Invalid, dispatch: Dispatch<Event>) =
        Props.SignedOut(
            Prop.Text("Something's wrong"),
            Prop.Text("Got invalid account. Please try signing in again."),
            Prop.Button("Retry") { dispatch(Event.Intent.SignIn) })

    private fun props(
        @Suppress("UNUSED_PARAMETER") model: Model.Blank,
        dispatch: Dispatch<Event>
    ) = Props.SignedOut(
        title = Prop.Text(text = "Welcome to Slaccount"),
        desc = Prop.Text(text = "Please sign in with your Slack account to display your personal info"),
        button = Prop.Button(text = "Sign into Slack", isEnabled = true) {
            dispatch(Event.Intent.SignIn)
        })

    private fun props(
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

    private fun props(
        model: Model.Authorized,
        dispatch: Dispatch<Event>
    ) = Props.SigningIn(
        title = Prop.Text("Welcome to Slaccount"),
        progress = Prop.Progress(value = Math.random().toFloat()),
        cancel = Prop.Button(text = "Cancel") {
            dispatch(Event.Intent.SignInCancel)
        },
        Prop.Text("Almost done!"),
        Prop.Text("We're waiting for Slack to give us info about your account."),
        Prop.Text("Here's your access token:"),
        Prop.Text(model.accessToken)
    )

    // endregion

    // region Update

    data class Ctx(val slack: Slack, val platform: Platform)

    /**
     * This type represents a piece of data sent from the outside world to this program,
     * for example the press of a button from a user, or a response from a web-service */
    sealed interface Event {

        /** User intent e.g. when user presses a button */
        sealed interface Intent : Event {
            object SignOut : Event
            object SignIn : Event
            object SignInCancel : Event
        }

        /** Result of an external operation e.g. response of a web-service call */
        sealed interface Outcome : Event {
            data class AuthScopeStatus(val status: Slack.AuthenticationScopeStatus) : Event
            data class AccessToken(val credentialsResult: Result<Slack.Credentials>) : Event
            data class UserInfo(val userInfoResult: Result<Slack.UserInfo>) : Event
        }

        data class Retrieved(val subEvent: SlackRetrievedAccount.Event) : Event
    }

    fun init() = Change<Model, _>(Model.Blank, none<Event>())

    fun makeUpdate(
        ctx: Ctx
    ): (Event, Model) -> Change<Model, Event> = { event, model ->
        update(ctx, event, model)
    }

    private fun update(
        ctx: Ctx,
        event: Event,
        model: Model
    ): Change<Model, Event> = when (model) {
        is Model.Blank -> change(ctx, model, event)
        is Model.Pending -> change(ctx, model, event)
        is Model.Authorized -> change(ctx, model, event)
        is Model.Invalid -> change(ctx, model, event)
        is Model.Retrieved -> change(ctx, model, event)
    }

    private fun change(
        ctx: Ctx,
        model: Model.Blank,
        event: Event
    ): Change<Model, Event> = model.run {
        check(event is Event.Intent.SignIn)
        Change(Model.Pending()) { dispatch ->
            ctx.platform.logi { "Got $event" }
            ctx.slack.requestAuthScopes().collect { status ->
                ctx.platform.logi { "Got status $status" }
                dispatch(Event.Outcome.AuthScopeStatus(status))
            }
        }
    }

    private fun change(
        ctx: Ctx,
        model: Model.Pending,
        event: Event
    ) = when (event) {

        is Event.Outcome.AuthScopeStatus -> when (event.status) {
            Slack.AuthenticationScopeStatus.Route.Init, Slack.AuthenticationScopeStatus.Route.Started -> Change(
                Model.Pending()
            )

            is Slack.AuthenticationScopeStatus.Route.Exposed -> updateOnAuthCodeRouteExposed(ctx, event.status)
            is Slack.AuthenticationScopeStatus.Success -> updateOnAuthCodeSuccess(ctx, model, event.status)
            is Slack.AuthenticationScopeStatus.Failure -> updateOnAuthScopeFailure(event.status, ctx)
        }

        Event.Intent.SignInCancel -> Change(Model.Blank) { ctx.slack.tearDownLogin() }

        is Event.Outcome.AccessToken -> event.credentialsResult.fold(
            onSuccess = { credentials ->
                val token = credentials.userToken
                Change(Model.Authorized(token)) { dispatch ->
                    val result = ctx.slack.retrieveUser(credentials)
                    val outcome = Event.Outcome.UserInfo(result)
                    dispatch(outcome)
                }
            },
            onFailure = { thrown ->
                Change(Model.Invalid(thrown)) {
                    ctx.platform.logi(thrown) { "Failure exchanging code for access token" }
                }
            }
        )

        else -> error("Invalid event for pending model: $event")
    }

    private fun updateOnAuthCodeSuccess(
        ctx: Ctx,
        model: Model,
        status: Slack.AuthenticationScopeStatus.Success
    ): Change<Model, Event> = run {
        check(model is Model.Pending)
        checkNotNull(model.redirectUri)
        Change(Model.Pending(model.redirectUri)) { dispatch ->
            ctx.platform.getEnvVar("SLACK_CLIENT_SECRET")
                .flatMapCatching { clientSecret ->
                    ctx.slack.exchangeCodeForCredentials(
                        code = status.code,
                        clientId = SLACK_CLIENT_ID,
                        clientSecret = clientSecret,
                        redirectUri = model.redirectUri
                    )
                }.let { credentialsResult ->
                    Event.Outcome.AccessToken(credentialsResult)
                }.also(dispatch)
        }
    }

    private fun updateOnAuthScopeFailure(
        status: Slack.AuthenticationScopeStatus.Failure,
        ctx: Ctx
    ): Change<Model, Event> =
        Change(Model.Invalid(status.throwable)) {
            ctx.slack.tearDownLogin()
        }

    private fun updateOnAuthCodeRouteExposed(
        ctx: Ctx,
        status: Slack.AuthenticationScopeStatus.Route.Exposed
    ): Change<Model, Event> =
        status.url.let { exposedUrl -> // URL-encode exposed URL
            val decodedRedirectUri = exposedUrl.toExternalForm()
            val charset = Charset.defaultCharset().name()
            URLEncoder.encode(decodedRedirectUri, charset)
        }.let { redirectUri -> // Make authorization-URI suffix
            val clientId = SLACK_CLIENT_ID
            val scopeParam = "incoming-webhook,commands"
            val userScopeParam = "users:read,users:read.email"
            "?scope=$scopeParam&user_scope=$userScopeParam&client_id=$clientId&redirect_uri=$redirectUri" to redirectUri
        }.let { (urlSuffix, redirectUri) -> // Take suffix, build full URL, make change
            Change(Model.Pending(redirectUri)) {
                val baseAuthUrl = ctx.slack.authUrlStr
                val url = "$baseAuthUrl$urlSuffix"
                ctx.platform.apply {
                    logi { "Callback server exposed at URL: ${status.url}" }
                    openUri(url)
                }
            }
        }

    private fun change(
        ctx: Ctx,
        model: Model.Authorized,
        event: Event
    ): Change<Model, Event> =
        when (event) {

            Event.Intent.SignInCancel -> Change(Model.Blank) { ctx.slack.tearDownLogin() }

            is Event.Outcome.UserInfo -> let {
                model.accessToken
            }.let { accessToken ->
                event.userInfoResult.fold(
                    onSuccess = { info ->
                        val (subModel, subEffect) = SlackRetrievedAccount.init(ctx.platform, accessToken, info)
                        Change(Model.Retrieved(subModel), map(subEffect) { subEvent ->
                            Event.Retrieved(subEvent)
                        })
                    },
                    onFailure = { throwable ->
                        Change(Model.Invalid(IllegalStateException("Expected valid user info", throwable))) {
                            ctx.platform.logi(throwable) { "Error retrieving user info" }
                        }
                    }
                )
            }

            else -> error("Invalid event for authorized account: $event")
        }

    private fun change(
        ctx: Ctx,
        model: Model.Invalid,
        event: Event
    ): Change<Model, Event> = model.run {
        check(event is Event.Intent.SignIn)
        Change(Model.Pending()) { dispatch ->
            ctx.platform.logi { "Got $event" }
            ctx.slack.requestAuthScopes().collect { status ->
                ctx.platform.logi { "Got status $status" }
                dispatch(Event.Outcome.AuthScopeStatus(status))
            }
        }
    }

    private fun change(
        ctx: Ctx,
        model: Model.Retrieved,
        event: Event
    ): Change<Model, Event> = when (event) {
        is Event.Retrieved -> changeRetrievedOnSubEvent(ctx, model, event)
        Event.Intent.SignOut -> changeRetrievedOnSignOut(ctx, model)
        else -> error("Unknown event for retrieved account: $event")
    }

    private fun changeRetrievedOnSubEvent(
        ctx: Ctx,
        model: Model.Retrieved,
        event: Event.Retrieved
    ): Change<Model, Event> = run {
        val subCtx = object : Slack by ctx.slack, Platform by ctx.platform {}
        val subChange = SlackRetrievedAccount.update(subCtx, model.subModel, event.subEvent)
        val newModel = Model.Retrieved(subChange.model)
        fun mapSubEvent(subEvent: SlackRetrievedAccount.Event) =
            if (subEvent is SlackRetrievedAccount.Event.SignOut) {
                Event.Intent.SignOut
            } else {
                Event.Retrieved(subEvent)
            }

        val newEffect = map(subChange.effect) { mapSubEvent(it) }
        Change(newModel, newEffect)
    }

    private fun changeRetrievedOnSignOut(
        ctx: Ctx,
        model: Model.Retrieved
    ): Change<Model, Event> =
        Change(Model.Blank) {
            ctx.slack.tearDownLogin()
            ctx.slack.revoke(model.subModel.accessToken)
        }

    // endregion
}


