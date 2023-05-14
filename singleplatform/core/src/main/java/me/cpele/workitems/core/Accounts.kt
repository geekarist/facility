package me.cpele.workitems.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.cpele.workitems.core.Slack.AuthStatus
import oolong.effect
import oolong.effect.none
import java.net.URLEncoder
import java.nio.charset.Charset

/**
 * This program implements user accounts and related functions, including:
 * - Credentials, personal info
 * - Authentication, inspection
 */
object Accounts {
    fun init(): Change<Model, Event> = Model.ProviderSelection to none()

    fun makeUpdate(
        slack: Slack, platform: Platform
    ): (Event, Model) -> Change<Model, Event> =
        { event: Event, model: Model ->
            when (event) {
                is Event.InspectProvider -> handle(event, slack, platform)
                is Event.InitiateLogin -> handle(event, model, platform, slack)
                is Event.GotAuthScopeStatus -> handle(event, model, platform, slack)
                is Event.GotLoginResult -> handle(event, model, platform)
                Event.DismissProvider -> handleDismissProvider(slack)
                is Event.GotAccessToken -> TODO()
            }
        }

    private fun handleDismissProvider(
        slack: Slack
    ): Change<Model, Event> =
        Model.ProviderSelection to effect {
            slack.tearDownLogin()
        }

    private fun handle(
        event: Event.GotLoginResult,
        model: Model,
        platform: Platform
    ): Change<Model, Event> =
        model to effect {
            platform.logi { "Got login result: ${event.tokenResult}" }
        }

    private fun handle(
        event: Event.InspectProvider,
        slack: Slack,
        platform: Platform
    ) = Model.ProviderInspection(provider = event.provider) to
            if (event.provider is Model.Provider.Slack) {
                effect { dispatch ->
                    platform.logi { "Got message: $event" }
                    slack.requestAuthScopes().collect { status ->
                        dispatch(Event.GotAuthScopeStatus(status))
                    }
                }
            } else {
                {
                    TODO("Sign in to other providers")
                }
            }

    private fun handle(
        event: Event.InitiateLogin,
        model: Model,
        platform: Platform,
        slack: Slack
    ) = model to when (event.provider) {
        is Model.Provider.Slack -> effect<Event> { _ ->
            val clientId = "961165435895.5012210604118"
            val status = event.provider.status
            check(status is AuthStatus.Route.Exposed) {
                val simpleName = AuthStatus.Route.Exposed::class.simpleName
                "Status should be $simpleName but is $status"
            }
            val decodedRedirectUri = status.url.toExternalForm()
            val redirectUri = withContext(Dispatchers.IO) {
                val charset = Charset.defaultCharset().name()
                URLEncoder.encode(decodedRedirectUri, charset)
            }
            val authUrl = slack.authUrlStr
            val scope = "incoming-webhook,commands"
            val url = "$authUrl?scope=$scope&client_id=$clientId&redirect_uri=$redirectUri"
            platform.openUri(url = url)
        }

        Model.Provider.Jira -> TODO()
        Model.Provider.GitHub -> TODO()
    }

    private fun handle(
        event: Event.GotAuthScopeStatus,
        model: Model,
        platform: Platform,
        slack: Slack
    ): Change<Model, Event> {

        val logEffect = effect<Event> {
            platform.logi { "Got login status: $event" }
        }
        val exchangeEffect = { code: String ->
            effect<Event> { dispatch ->
                val accessToken = slack.exchangeCodeForToken(code)
                dispatch(Event.GotAccessToken(accessToken))
            }
        }

        return when (event.status) {

            is AuthStatus.Route.Started,
            is AuthStatus.Route.Exposed
            -> model.let {
                check(it is Model.ProviderInspection)
                it
            }.let { providerInspectionModel ->
                check(providerInspectionModel.provider is Model.Provider.Slack)
                val provider = providerInspectionModel.provider.copy(event.status)
                providerInspectionModel.copy(provider = provider)
            } to logEffect

            is AuthStatus.Route.Init,
            is AuthStatus.Failure -> model to logEffect

            is AuthStatus.Success -> model to exchangeEffect(event.status.code)
        }
    }

    fun view(model: Model, dispatch: (Event) -> Unit) =
        Props(
            dialog = model
                .let { it as? Model.ProviderInspection }
                ?.let { inspectionStep ->
                    val provider = inspectionStep.provider
                    val isButtonEnabled =
                        provider is Model.Provider.Slack &&
                                provider.status is AuthStatus.Route.Exposed
                    Props.Dialog.of(
                        button = Props.Button("Log in...") {
                            dispatch(Event.InitiateLogin(provider))
                        },
                        isButtonEnabled = isButtonEnabled,
                        onClose = { dispatch(Event.DismissProvider) },
                        provider.description
                    )
                },
            buttons = listOf(
                Props.Button("Slack") { dispatch(Event.InspectProvider(Model.Provider.Slack(AuthStatus.Route.Init))) },
                Props.Button("Jira") { dispatch(Event.InspectProvider(Model.Provider.Jira)) },
                Props.Button("GitHub") { dispatch(Event.InspectProvider(Model.Provider.GitHub)) },
            )
        )

    sealed interface Event {
        data class InspectProvider(val provider: Model.Provider) : Event
        data class InitiateLogin(val provider: Model.Provider) : Event
        data class GotAuthScopeStatus(val status: AuthStatus) : Event
        data class GotLoginResult(val tokenResult: Result<String>) : Event
        data class GotAccessToken(val accessToken: Result<String>) : Event
        object DismissProvider : Event
    }

    sealed interface Model {
        object ProviderSelection : Model
        data class ProviderInspection(val provider: Provider) : Model

        sealed class Provider(
            val description: String
        ) {
            data class Slack(val status: AuthStatus) : Provider(
                description = "Slack lets you use reactions to tag certain messages, turning them into work items",
            )

            object Jira : Provider(description = "Jira tickets assigned to you appear as work items")
            object GitHub : Provider(description = "GitHub issues or PRs assigned to you appear as work items")
        }
    }

    data class Props(
        val dialog: Dialog? = null, val buttons: List<Button>
    ) {
        data class Dialog(
            val texts: Collection<String>,
            val isButtonEnabled: Boolean,
            val button: Button,
            val onClose: () -> Unit
        ) {
            companion object
        }

        data class Button(val text: String, val onClick: () -> Unit)
    }

    private fun Props.Dialog.Companion.of(
        button: Props.Button,
        isButtonEnabled: Boolean,
        onClose: () -> Unit,
        vararg texts: String
    ): Props.Dialog = Props.Dialog(
        texts = texts.toList(),
        isButtonEnabled = isButtonEnabled,
        button = button,
        onClose = onClose
    )
}
