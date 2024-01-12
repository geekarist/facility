package me.cpele.facility.core.programs

// TODO: Don't use Java URL encoder
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.cpele.facility.core.framework.Change
import me.cpele.facility.core.framework.Prop
import me.cpele.facility.core.framework.effects.Platform
import me.cpele.facility.core.framework.effects.Preferences
import me.cpele.facility.core.framework.effects.Slack
import me.cpele.facility.core.framework.effects.Store
import me.cpele.facility.core.framework.flatMapCatching
import oolong.Dispatch
import oolong.dispatch.contramap
import oolong.effect.map
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.min

private const val SLACK_CLIENT_ID = "961165435895.5012210604118"

private const val STORAGE_KEY = "slaccount-model"

object SlackAccount {

    // region Model

    /**
     * This model represents a Slack user account.
     */
    @Serializable
    sealed interface Model {
        /** Authentication process wasn't even started */
        @Serializable
        object Blank : Model

        /** Authentication started but not complete */
        @Serializable
        data class Pending(val redirectUri: String? = null, val job: Job? = null) : Model

        /** Authentication failed at some point */
        @Serializable
        data class Invalid(@Contextual val throwable: Throwable) : Model

        /** Authentication was successful */
        @Serializable
        data class Authorized(val credentials: Slack.Credentials) : Model {
            val accessToken: String = credentials.userToken
        }

        @Serializable
        data class Retrieved(val subModel: SlackRetrievedAccount.Model) : Model
    }

    // endregion

    // region View

    sealed interface Props {
        val windowTitle: Prop.Text
        val onWindowClose: () -> Unit

        data class SignedOut(
            override val windowTitle: Prop.Text,
            override val onWindowClose: () -> Unit,
            val title: Prop.Text,
            val desc: Prop.Text,
            val button: Prop.Button
        ) : Props

        data class SigningIn(
            override val windowTitle: Prop.Text,
            override val onWindowClose: () -> Unit,
            val title: Prop.Text,
            val progress: Prop.Progress,
            val cancel: Prop.Button,
            val statuses: List<Prop.Text>
        ) : Props {
            companion object {
                operator fun invoke(
                    windowTitle: Prop.Text,
                    onWindowClose: () -> Unit,
                    title: Prop.Text,
                    progress: Prop.Progress,
                    cancel: Prop.Button,
                    vararg status: Prop.Text
                ) = SigningIn(windowTitle, onWindowClose, title, progress, cancel, status.asList())
            }
        }

        data class Retrieved(
            override val windowTitle: Prop.Text,
            override val onWindowClose: () -> Unit,
            val subProps: SlackRetrievedAccount.Props
        ) : Props
    }

    fun view(model: Model, dispatch: Dispatch<Event>): Props = when (model) {
        is Model.Blank -> props(model, dispatch)
        is Model.Invalid -> props(model, dispatch)
        is Model.Pending -> props(model, dispatch)
        is Model.Authorized -> props(model, dispatch)
        is Model.Retrieved -> props(model, dispatch)
    }

    private fun props(@Suppress("UNUSED_PARAMETER") model: Model.Invalid, dispatch: Dispatch<Event>) =
        Props.SignedOut(
            windowTitle = Prop.Text("Invalid account | Slaccount"),
            onWindowClose = { dispatch(Event.Intent.Persist) },
            title = Prop.Text("Something's wrong"),
            desc = Prop.Text("Got invalid account. Please try signing in again."),
            button = Prop.Button("Retry") { dispatch(Event.Intent.SignIn) })

    private fun props(
        @Suppress("UNUSED_PARAMETER") model: Model.Blank,
        dispatch: Dispatch<Event>
    ) = Props.SignedOut(
        windowTitle = Prop.Text("Blank account | Slaccount"),
        onWindowClose = { dispatch(Event.Intent.Persist) },
        title = Prop.Text(text = "Welcome to Slaccount"),
        desc = Prop.Text(text = "Please sign in with your Slack account to display your personal info"),
        button = Prop.Button(text = "Sign into Slack", isEnabled = true) {
            dispatch(Event.Intent.SignIn)
        })

    private fun props(
        @Suppress("UNUSED_PARAMETER") model: Model.Pending,
        dispatch: Dispatch<Event>
    ) = Props.SigningIn(
        windowTitle = Prop.Text("Pending account | Slaccount"),
        onWindowClose = { dispatch(Event.Intent.Persist) },
        title = Prop.Text("Welcome to Slaccount"),
        progress = Prop.Progress(value = Math.random().toFloat()),
        cancel = Prop.Button(text = "Cancel") {
            dispatch(Event.Intent.SignInCancel())
        },
        Prop.Text("We need your permission to let Slack give us info about you."),
        Prop.Text("Waiting for you to sign into Slack through a web-browser window...")
    )

    private fun props(
        model: Model.Authorized,
        dispatch: Dispatch<Event>
    ) = Props.SigningIn(
        windowTitle = Prop.Text("Authorized account | Slaccount"),
        onWindowClose = { dispatch(Event.Intent.Persist) },
        title = Prop.Text("Welcome to Slaccount"),
        progress = Prop.Progress(value = Math.random().toFloat()),
        cancel = Prop.Button(text = "Cancel") {
            dispatch(Event.Intent.SignInCancel())
        },
        Prop.Text("Almost done!"),
        Prop.Text("We're waiting for Slack to give us info about your account."),
        Prop.Text("Here's your access token:"),
        Prop.Text(model.accessToken)
    )

    private fun props(
        model: Model.Retrieved,
        dispatch: (Event) -> Unit
    ): Props = Props.Retrieved(
        windowTitle = Prop.Text("Retrieved account | Slaccount"),
        onWindowClose = { dispatch(Event.Intent.Persist) },
        SlackRetrievedAccount.view(
            model = model.subModel,
            dispatch = contramap(dispatch, Event::Retrieved)
        )
    )

    // endregion

    // region Update

    data class Ctx(
        val slack: Slack,
        val platform: Platform,
        val preferences: Preferences,
        val store: Store
    )

    /**
     * This type represents a piece of data sent from the outside world to this program,
     * for example the press of a button from a user, or a response from a web-service */
    sealed interface Event {

        /** User intent e.g. when user presses a button */
        sealed interface Intent : Event {
            object SignIn : Event
            data class SignInCancel(val persisting: Boolean = false) : Event
            object Persist : Event
            object Reset : Event
        }

        /** Result of an external operation e.g. response of a web-service call */
        sealed interface Outcome : Event {
            data class AuthScopeStatus(val status: Slack.Authorization, val job: Job) : Event
            data class AccessToken(val credentialsResult: Result<Slack.Credentials>) : Event
            data class UserInfo(val userInfoResult: Result<Slack.UserInfo>) : Event
            data class DeserializedModel(val model: Model) : Event
            object Persisted : Event
        }

        data class Retrieved(val subEvent: SlackRetrievedAccount.Event) : Event
    }

    fun init(ctx: Ctx) = Change<Model, Event>(Model.Pending()) { dispatch ->
        ctx.platform.logi { "Getting serialized model" }
        val serializedModel = ctx.store.getString(STORAGE_KEY) ?: Json.encodeToString(Model.Blank)
        ctx.platform.logi {
            val subStrMaxIdx = min(serializedModel.length - 1, 160)
            val subStr = serializedModel.substring(0..subStrMaxIdx)
            "Got serialized model: $subStr"
        }
        val deserializedModel = serializedModel.let { Json.decodeFromString<Model>(it) }
        ctx.platform.logi { "Deserialized model" }
        val event = Event.Outcome.DeserializedModel(deserializedModel)
        ctx.platform.logi { "Dispatching event: $event" }
        dispatch(event)
    }

    fun update(
        ctx: Ctx,
        event: Event,
        model: Model
    ): Change<Model, Event> = run {
        Logger.getAnonymousLogger().log(Level.INFO, "Got event: $event")
        when (event) {
            is Event.Intent.Persist -> changeOnPersistIntent(ctx, model)
            is Event.Intent.Reset -> Change(Model.Blank) { dispatch ->
                ctx.store.clear(STORAGE_KEY)
                if (model is Model.Pending) {
                    model.job?.cancel()
                }
                ctx.slack.tearDownLogin()
                dispatch(Event.Outcome.Persisted)
            }

            else -> when (model) {
                is Model.Blank -> initPending(ctx, event)
                is Model.Pending -> change(ctx, model, event)
                is Model.Authorized -> initNextFromAuthorized(ctx, model, event)
                is Model.Invalid -> change(ctx, model, event)
                is Model.Retrieved -> change(ctx, model, event)
            }
        }
    }

    private fun changeOnPersistIntent(
        ctx: Ctx,
        model: Model
    ): Change<Model, Event> = Change(model) { dispatch: (Event) -> Unit ->
        val persistableModel = when (model) {
            is Model.Retrieved,
            is Model.Authorized -> model

            Model.Blank,
            is Model.Invalid,
            is Model.Pending -> Model.Blank
        }
        val serializedModel = Json.encodeToString(persistableModel)
        ctx.platform.logi {
            val subStrMaxIdx = min(serializedModel.length - 1, 160)
            val subStr = serializedModel.substring(0..subStrMaxIdx)
            "Storing serialized model: $subStr"
        }
        ctx.store.putString(STORAGE_KEY, serializedModel)

        Logger.getAnonymousLogger().log(Level.INFO, "Model persisted, now dispatch")
        if (model is Model.Pending) {
            Logger.getAnonymousLogger().log(Level.INFO, "Model is pending ⇒ dispatch cancel")
            dispatch(Event.Intent.SignInCancel(persisting = true))
        } else {
            Logger.getAnonymousLogger()
                .log(Level.INFO, "Finished changing model ⇒ dispatch ${Event.Outcome.Persisted}")
            dispatch(Event.Outcome.Persisted)
        }
    }

    private fun initPending(
        ctx: Ctx,
        event: Event
    ): Change<Model, Event> = run {
        check(event is Event.Intent.SignIn)
        Change(Model.Pending()) { dispatch ->
            ctx.platform.logi { "Got $event" }
            launch {
                ctx.slack.requestAuthScopes().collect { status ->
                    ctx.platform.logi { "Got status $status" }
                    dispatch(Event.Outcome.AuthScopeStatus(status, coroutineContext.job))
                }
            }.join()
        }
    }

    private fun change(
        ctx: Ctx,
        model: Model.Pending,
        event: Event
    ): Change<Model, Event> = when (event) {
        is Event.Outcome.AuthScopeStatus -> changeFromPendingOnAuthStatus(ctx, model, event)
        is Event.Intent.SignInCancel -> initBlank(ctx, event)
        is Event.Outcome.AccessToken -> initNextFromPendingOnAccess(ctx, event)
        is Event.Outcome.DeserializedModel -> changeFromPendingOnDeserialized(event, ctx)
        else -> error("Invalid event for pending model: $event")
    }

    private fun changeFromPendingOnDeserialized(
        event: Event.Outcome.DeserializedModel,
        ctx: Ctx
    ): Change<Model, Event> =
        Change(event.model) {
            ctx.platform.logi { "Changing to deserialized model: ${event.model}" }
        }

    private fun initNextFromPendingOnAccess(
        ctx: Ctx,
        event: Event.Outcome.AccessToken
    ): Change<Model, Event> = event.credentialsResult.fold(
        onSuccess = { credentials -> initAuthorized(ctx, credentials) },
        onFailure = { thrown -> initInvalid(ctx, thrown) }
    )

    private fun initInvalid(
        ctx: Ctx,
        thrown: Throwable
    ): Change<Model, Event> =
        Change(Model.Invalid(thrown)) {
            ctx.platform.logi(thrown) { "Failure exchanging code for access token" }
        }

    private fun initAuthorized(
        ctx: Ctx,
        credentials: Slack.Credentials
    ): Change<Model, Event> =
        Change(Model.Authorized(credentials)) { dispatch ->
            val result = ctx.slack.retrieveUser(credentials)
            val outcome = Event.Outcome.UserInfo(result)
            dispatch(outcome)
        }

    private fun changeFromPendingOnAuthStatus(
        ctx: Ctx,
        model: Model.Pending,
        event: Event.Outcome.AuthScopeStatus
    ): Change<Model, Event> = run {
        when (event.status) {
            Slack.Authorization.Requested,
            Slack.Authorization.Route.Init,
            Slack.Authorization.Route.Started ->
                Change(Model.Pending(job = event.job))

            is Slack.Authorization.Route.Exposed ->
                changeFromPendingOnAuthCodeRouteExposed(ctx, model, event.status, event.job)

            is Slack.Authorization.Success ->
                changeFromPendingOnAuthCodeSuccess(ctx, model, event.status, event.job)

            is Slack.Authorization.Failure ->
                initInvalidFromPendingOnAuthorizationFailure(event.status, ctx)
        }
    }

    private fun changeFromPendingOnAuthCodeSuccess(
        ctx: Ctx,
        model: Model.Pending,
        status: Slack.Authorization.Success,
        job: Job
    ): Change<Model, Event> = run {
        checkNotNull(model.redirectUri)
        Change(Model.Pending(model.redirectUri, job)) { dispatch ->
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

    private fun initInvalidFromPendingOnAuthorizationFailure(
        status: Slack.Authorization.Failure,
        ctx: Ctx
    ): Change<Model, Event> =
        Change(Model.Invalid(status.throwable)) {
            ctx.slack.tearDownLogin()
        }

    private fun changeFromPendingOnAuthCodeRouteExposed(
        ctx: Ctx,
        model: Model.Pending,
        status: Slack.Authorization.Route.Exposed,
        job: Job
    ): Change<Model, Event> = model.run {
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
            Change(Model.Pending(redirectUri, job)) {
                val baseAuthUrl = ctx.slack.authUrlStr
                val url = "$baseAuthUrl$urlSuffix"
                ctx.platform.apply {
                    logi { "Callback server exposed at URL: ${status.url}" }
                    openUri(url)
                }
            }
        }
    }

    private fun initNextFromAuthorized(
        ctx: Ctx,
        model: Model.Authorized,
        event: Event
    ): Change<Model, Event> =
        when (event) {

            is Event.Intent.SignInCancel -> initBlank(ctx, event)

            is Event.Outcome.UserInfo -> let {
                model.credentials
            }.let { credentials ->
                event.userInfoResult.fold(
                    onSuccess = { info ->
                        val (subModel, subEffect) = SlackRetrievedAccount.init(ctx.platform, credentials, info)
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

    private fun initBlank(ctx: Ctx, event: Event.Intent.SignInCancel): Change<Model, Event> =
        Change(Model.Blank) { dispatch ->
            Logger.getAnonymousLogger().log(Level.INFO, "Tearing down login")
            ctx.slack.tearDownLogin()
            if (event.persisting) {
                dispatch(Event.Outcome.Persisted)
            }
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
                dispatch(Event.Outcome.AuthScopeStatus(status, coroutineContext.job))
            }
        }
    }

    private fun change(
        ctx: Ctx,
        model: Model.Retrieved,
        event: Event
    ): Change<Model, Event> = run {
        check(event is Event.Retrieved)
        // Check sub-event for interception
        when (event.subEvent) {
            SlackRetrievedAccount.Event.Refresh -> initAuthorized(ctx, model.subModel.credentials)
            SlackRetrievedAccount.Event.SignOut -> initBlankOnSignOut(ctx, model.subModel.accessToken)
            is SlackRetrievedAccount.Event.FetchedUserImage -> {
                val subCtx = object : Slack by ctx.slack, Platform by ctx.platform {}
                val (subModel, subEffect) = SlackRetrievedAccount.update(subCtx, model.subModel, event.subEvent)
                Change(model = Model.Retrieved(subModel), effect = map(subEffect) { Event.Retrieved(it) })
            }
        }
    }


    private fun initBlankOnSignOut(
        ctx: Ctx,
        accessToken: String
    ): Change<Model, Event> =
        Change(Model.Blank) {
            ctx.slack.tearDownLogin()
            ctx.slack.revoke(accessToken)
        }

    // endregion
}


