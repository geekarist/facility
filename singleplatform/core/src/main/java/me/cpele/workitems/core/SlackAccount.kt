package me.cpele.workitems.core

import oolong.Dispatch
import oolong.Effect
import oolong.effect.none

object SlackAccount {

    //region Model

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

        /** Authentication retrieved */
        data class Retrieved(
            val accessToken: String,
            val id: String,
            val image: String,
            val imageBuffer: ByteArray? = null,
            val name: String,
            val realName: String,
            val email: String,
            val presence: String
        ) : Model {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Retrieved

                if (accessToken != other.accessToken) return false
                if (id != other.id) return false
                if (image != other.image) return false
                if (imageBuffer != null) {
                    if (other.imageBuffer == null) return false
                    if (!imageBuffer.contentEquals(other.imageBuffer)) return false
                } else if (other.imageBuffer != null) return false
                if (name != other.name) return false
                if (realName != other.realName) return false
                if (email != other.email) return false
                return presence == other.presence
            }

            override fun hashCode(): Int {
                var result = accessToken.hashCode()
                result = 31 * result + id.hashCode()
                result = 31 * result + image.hashCode()
                result = 31 * result + (imageBuffer?.contentHashCode() ?: 0)
                result = 31 * result + name.hashCode()
                result = 31 * result + realName.hashCode()
                result = 31 * result + email.hashCode()
                result = 31 * result + presence.hashCode()
                return result
            }

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
            val signOut: Prop.Button
        ) : Props
    }

    fun view(model: Model, dispatch: Dispatch<Event>): Props = when (model) {
        is Model.Blank -> view(model, dispatch)
        is Model.Invalid -> TODO()
        is Model.Pending -> view(model, dispatch)
        is Model.Authorized -> view(model, dispatch)
        is Model.Retrieved -> view(model, dispatch)
    }

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
            image = model.imageBuffer?.let { Prop.Image(it) },
            name = Prop.Text(model.realName),
            availability = Prop.Text(model.presence),
            token = Prop.Text("Access token: ${model.accessToken}"),
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
        is Event.Outcome.AuthScopeStatus -> update(ctx, event)
        Event.Intent.SignInCancel -> Change(Model.Blank) { ctx.slack.tearDownLogin() }
        is Event.Outcome.AccessToken -> update(ctx, event)
        is Event.Outcome.UserInfo -> update(ctx, model, event)
        is Event.Outcome.FetchedUserImage -> update(ctx, model, event)
        Event.Intent.SignOut -> Change(model) { ctx.platform.logi { "TODO: Handle $event" } }
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
            onSuccess = { Change(model.copy(imageBuffer = it)) },
            onFailure = { throwable ->
                Change(model) {
                    ctx.platform.logi(throwable) { "Failed retrieving image ${model.image}" }
                }
            }
        )
    }

    private fun update(ctx: Ctx, model: Model, event: Event.Outcome.UserInfo): Change<Model, Event> {
        check(model is Model.Authorized) {
            "Model must be ${Model.Authorized::class.simpleName} but is: $model"
        }
        val accessToken = model.accessToken
        val changedModel = Model.Retrieved.of(accessToken, event)
        val effect: Effect<Event> = if (changedModel is Model.Retrieved) {
            { dispatch ->
                val imageUrl = changedModel.image
                val bufferResult = ctx.platform.fetch(imageUrl)
                dispatch(Event.Outcome.FetchedUserImage(bufferResult))
            }
        } else none()
        return Change(model = changedModel, effect = effect)
    }

    private fun Model.Retrieved.Companion.of(
        accessToken: String,
        event: Event.Outcome.UserInfo
    ): Model = event.userInfoResult.fold(
        onSuccess = { info ->
            Model.Retrieved(
                accessToken = accessToken,
                id = info.id,
                image = info.image,
                name = info.name,
                realName = info.realName,
                email = info.email,
                presence = info.presence
            )
        },
        onFailure = { throwable ->
            Model.Invalid(IllegalStateException("Expected valid user info", throwable))
        }
    )

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
        onFailure = { Change(Model.Invalid(it)) }
    )

    private fun update(
        ctx: Ctx,
        event: Event.Intent.SignIn
    ): Change<Model, Event> = Change(Model.Pending) { dispatch ->
        ctx.platform.logi { "Got $event" }
        ctx.slack.requestAuthScopes().collect { status ->
            ctx.platform.logi { "Got status $status" }
            dispatch(Event.Outcome.AuthScopeStatus(status))
        }
    }

    private fun update(
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
    //endregion
}


