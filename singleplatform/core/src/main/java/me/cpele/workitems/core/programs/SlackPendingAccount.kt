package me.cpele.workitems.core.programs

import me.cpele.workitems.core.framework.*
import oolong.Dispatch
import java.net.URLEncoder
import java.nio.charset.Charset

private const val SLACK_CLIENT_ID = "961165435895.5012210604118"

object SlackPendingAccount {

    data class Model(val redirectUri: String? = null)

    data class Props(
        val title: Prop.Text,
        val progress: Prop.Progress,
        val cancel: Prop.Button,
        val statuses: List<Prop.Text>
    ) {
        companion object {
            operator fun invoke(
                title: Prop.Text,
                progress: Prop.Progress,
                cancel: Prop.Button,
                vararg status: Prop.Text
            ) = Props(title, progress, cancel, status.asList())
        }
    }

    sealed interface Event {
        data class AuthScopeStatus(val status: Slack.AuthenticationScopeStatus) : Event
        object SignInCancel : Event
        data class AccessToken(val credentialsResult: Result<Slack.Credentials>) : Event
    }

    fun view(
        @Suppress("UNUSED_PARAMETER") model: Model,
        dispatch: Dispatch<Event>
    ) = Props(
        title = Prop.Text("Welcome to Slaccount"),
        progress = Prop.Progress(value = Math.random().toFloat()),
        cancel = Prop.Button(text = "Cancel") {
            dispatch(Event.SignInCancel)
        },
        Prop.Text("We need your permission to let Slack give us info about you."),
        Prop.Text("Waiting for you to sign into Slack through a web-browser window...")
    )

    fun <Ctx> init(
        ctx: Ctx,
        event: Event
    ): Change<Model, Event>
            where Ctx : Slack, Ctx : Platform = run {
        Change(Model()) { dispatch ->
            ctx.logi { "Got $event" }
            ctx.requestAuthScopes().collect { status ->
                ctx.logi { "Got status $status" }
                dispatch(Event.AuthScopeStatus(status))
            }
        }
    }

    fun <Ctx> update(
        ctx: Ctx,
        model: Model,
        event: Event
    ) where Ctx : Slack, Ctx : Platform = when (event) {

        is Event.AuthScopeStatus -> when (event.status) {
            Slack.AuthenticationScopeStatus.Route.Init,
            Slack.AuthenticationScopeStatus.Route.Started -> Change(Model())

            is Slack.AuthenticationScopeStatus.Route.Exposed -> updateOnAuthCodeRouteExposed(ctx, event.status)
            is Slack.AuthenticationScopeStatus.Success -> updateOnAuthCodeSuccess(ctx, model, event.status)
            is Slack.AuthenticationScopeStatus.Failure -> {
                // Handle upstream ⇒ no op
                Change(model)
                // Change<SlackAccount.Model, SlackAccount.Event>(
                //     SlackAccount.Model.Invalid(event.status.throwable)
                // ) {
                //     ctx.tearDownLogin()
                // }
            }
        }

        // Handled upstream ⇒ no op
        is Event.AccessToken, // -> {
            // event.credentialsResult.fold(
            //     onSuccess = { credentials ->
            //         // val token = credentials.userToken
            //         // Change(SlackAccount.Model.Authorized(token)) { dispatch ->
            //         //     val result = ctx.retrieveUser(credentials)
            //         //     val outcome = SlackAccount.Event.Outcome.UserInfo(result)
            //         //     dispatch(outcome)
            //         // }
            //     },
            //     onFailure = { thrown ->
            //         // Change(SlackAccount.Model.Invalid(thrown)) {
            //         //     ctx.logi(thrown) { "Failure exchanging code for access token" }
            //         // }
            //     }
            // )
            // }
        Event.SignInCancel // TODO: Tear down login // Change(SlackAccount.Model.Blank) { ctx.tearDownLogin() }
        -> Change(model)
    }

    private fun <Ctx> updateOnAuthCodeSuccess(
        ctx: Ctx,
        model: Model,
        status: Slack.AuthenticationScopeStatus.Success
    ): Change<Model, Event>
            where Ctx : Slack,
                  Ctx : Platform =
        run {
            checkNotNull(model.redirectUri)
            Change(Model(model.redirectUri)) { dispatch ->
                ctx.getEnvVar("SLACK_CLIENT_SECRET")
                    .flatMapCatching { clientSecret ->
                        ctx.exchangeCodeForCredentials(
                            code = status.code,
                            clientId = SLACK_CLIENT_ID,
                            clientSecret = clientSecret,
                            redirectUri = model.redirectUri
                        )
                    }.let { credentialsResult ->
                        Event.AccessToken(credentialsResult)
                    }.also(dispatch)
            }
        }

    private fun <Ctx> updateOnAuthCodeRouteExposed(
        ctx: Ctx,
        status: Slack.AuthenticationScopeStatus.Route.Exposed
    ): Change<Model, Event> where Ctx : Slack,
                                  Ctx : Platform =
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
            Change(Model(redirectUri)) {
                val baseAuthUrl = ctx.authUrlStr
                val url = "$baseAuthUrl$urlSuffix"
                ctx.logi { "Callback server exposed at URL: ${status.url}" }
                ctx.openUri(url)
            }
        }
}