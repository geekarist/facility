package me.cpele.workitems.core.programs

import me.cpele.workitems.core.framework.Change
import me.cpele.workitems.core.framework.Platform
import me.cpele.workitems.core.framework.Prop
import me.cpele.workitems.core.framework.Slack
import oolong.Dispatch
import oolong.effect.none
// TODO: Don't use Java URL encoder
import java.net.URLEncoder
import java.nio.charset.Charset

private const val SLACK_CLIENT_ID = "961165435895.5012210604118"

object SlackAccount {

    //region Model

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

        /** Authentication retrieved */
        data class Retrieved(
            val accessToken: String,
            val id: String,
            val image: String,
            val imageBuffer: ImageBuffer? = null,
            val name: String,
            val realName: String,
            val email: String,
            val presence: String
        ) : Model {
            companion object
        }
    }

    //endregion

    //region View

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
            val email: Prop.Text,
            val signOut: Prop.Button
        ) : Props
    }

    fun view(model: Model, dispatch: Dispatch<Event>): Props = when (model) {
        is Model.Blank -> view(model, dispatch)
        is Model.Invalid -> view(model, dispatch)
        is Model.Pending -> view(model, dispatch)
        is Model.Authorized -> view(model, dispatch)
        is Model.Retrieved -> view(model, dispatch)
    }

    private fun view(@Suppress("UNUSED_PARAMETER") model: Model.Invalid, dispatch: Dispatch<Event>) =
        Props.SignedOut(
            Prop.Text("Something's wrong"),
            Prop.Text("Got invalid account. Please try signing in again."),
            Prop.Button("Retry") { dispatch(Event.Intent.SignIn) })

    private fun view(
        @Suppress("UNUSED_PARAMETER") model: Model.Blank,
        dispatch: Dispatch<Event>
    ) = Props.SignedOut(
        title = Prop.Text(text = "Welcome to Slaccount"),
        desc = Prop.Text(text = "Please sign in with your Slack account to display your personal info"),
        button = Prop.Button(text = "Sign into Slack", isEnabled = true) {
            dispatch(Event.Intent.SignIn)
        })

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

    private fun view(model: Model.Retrieved, dispatch: (Event) -> Unit): Props =
        Props.SignedIn(
            image = model.imageBuffer?.let { Prop.Image(it.array) },
            name = Prop.Text(model.realName),
            availability = Prop.Text(model.presence),
            token = Prop.Text("Access token: ${model.accessToken}"),
            email = Prop.Text("Email: ${model.email}"),
            signOut = Prop.Button("Sign out") { dispatch(Event.Intent.SignOut) }
        )

    //endregion

    //region Update

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
            data class AccessToken(val token: Result<String>) : Event
            data class UserInfo(val userInfoResult: Result<Slack.UserInfo>) : Event
            data class FetchedUserImage(val bufferResult: Result<ByteArray>) : Event
        }
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
    ): Change<Model, Event> = when (event) {
        is Event.Intent.SignIn -> update(ctx, event)
        is Event.Outcome.AuthScopeStatus -> update(ctx, model, event)
        Event.Intent.SignInCancel -> Change(Model.Blank) { ctx.slack.tearDownLogin() }
        is Event.Outcome.AccessToken -> update(ctx, event)
        is Event.Outcome.UserInfo -> update(ctx, model, event)
        is Event.Outcome.FetchedUserImage -> update(ctx, model, event)
        Event.Intent.SignOut -> update(ctx, model)
    }

    private fun update(
        ctx: Ctx,
        model: Model
    ): Change<Model, Event> =
        Change(Model.Blank) {
            check(model is Model.Retrieved)
            ctx.slack.tearDownLogin()
            ctx.slack.revoke(model.accessToken)
        }

    private fun update(
        ctx: Ctx,
        model: Model,
        event: Event.Outcome.FetchedUserImage
    ): Change<Model, Event> = run {
        check(model is Model.Retrieved) {
            "Model must be ${Model.Retrieved::class.simpleName} but is: $model"
        }
        event.bufferResult.fold(
            onSuccess = { Change(model.copy(imageBuffer = ImageBuffer(it))) },
            onFailure = { throwable ->
                Change(model) {
                    ctx.platform.logi(throwable) { "Failed retrieving image ${model.image}" }
                }
            }
        )
    }

    private fun update(ctx: Ctx, model: Model, event: Event.Outcome.UserInfo): Change<Model, Event> =
        let {
            check(model is Model.Authorized) {
                "Model must be authorized but is: $model"
            }
            model.accessToken
        }.let { accessToken ->
            event.userInfoResult.fold(
                onSuccess = { info ->
                    val newModel = Model.Retrieved( // Retrieved account
                        accessToken = accessToken,
                        id = info.id,
                        image = info.image,
                        name = info.name,
                        realName = info.realName,
                        email = info.email,
                        presence = info.presence
                    )
                    Change(newModel) { dispatch ->
                        // Move on to image retrieval
                        val imageUrl = newModel.image
                        val bufferResult = ctx.platform.fetch(imageUrl)
                        dispatch(Event.Outcome.FetchedUserImage(bufferResult))
                    }
                },
                onFailure = { throwable ->
                    Change(Model.Invalid(IllegalStateException("Expected valid user info", throwable))) {
                        ctx.platform.logi(throwable) { "Error retrieving user info" }
                    }
                }
            )
        }

    private fun update(
        ctx: Ctx,
        event: Event.Outcome.AccessToken
    ): Change<Model, Event> = event.token.fold(
        onSuccess = { token ->
            Change(Model.Authorized(token)) { dispatch ->
                val result = ctx.slack.retrieveUser(token)
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

    private fun update(
        ctx: Ctx,
        event: Event.Intent.SignIn
    ): Change<Model, Event> = Change(Model.Pending()) { dispatch ->
        ctx.platform.logi { "Got $event" }
        ctx.slack.requestAuthScopes().collect { status ->
            ctx.platform.logi { "Got status $status" }
            dispatch(Event.Outcome.AuthScopeStatus(status))
        }
    }

    private fun update(
        ctx: Ctx, model: Model, event: Event.Outcome.AuthScopeStatus
    ): Change<Model, Event> = when (event.status) {
        Slack.AuthenticationScopeStatus.Route.Init, Slack.AuthenticationScopeStatus.Route.Started -> Change(Model.Pending())
        is Slack.AuthenticationScopeStatus.Route.Exposed -> updateOnAuthCodeRouteExposed(ctx, event.status)
        is Slack.AuthenticationScopeStatus.Success -> updateOnAuthCodeSuccess(ctx, model, event.status)
        is Slack.AuthenticationScopeStatus.Failure -> updateOnAuthScopeFailure(event.status, ctx)
    }

    private fun updateOnAuthCodeSuccess(
        ctx: Ctx,
        model: Model,
        status: Slack.AuthenticationScopeStatus.Success
    ): Change<Model, Event> = run {
        check(model is Model.Pending)
        model
    }.let { pendingModel ->
        checkNotNull(pendingModel.redirectUri)
        Change(Model.Pending(pendingModel.redirectUri)) { dispatch ->
            ctx.platform.getEnvVar("SLACK_CLIENT_SECRET")
                .mapCatching { clientSecret ->
                    ctx.slack.exchangeCodeForToken(
                        code = status.code,
                        clientId = SLACK_CLIENT_ID,
                        clientSecret = clientSecret,
                        redirectUri = pendingModel.redirectUri
                    )
                }.let { accessTokenResult ->
                    val accessToken = accessTokenResult.getOrThrow()
                    Event.Outcome.AccessToken(accessToken)
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
            val scope = "openid,email,profile"
            "?response_type=code&scope=$scope&client_id=$clientId&redirect_uri=$redirectUri" to redirectUri
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

    //endregion
}


