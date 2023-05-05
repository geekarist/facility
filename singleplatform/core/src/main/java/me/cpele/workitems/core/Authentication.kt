package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.cpele.workitems.core.Slack.AuthStatus
import oolong.Dispatch
import oolong.Effect
import oolong.effect
import oolong.effect.none
import java.net.URLEncoder
import java.nio.charset.Charset

/**
 * This program implements the authentication process.
 */
object Authentication {
    fun init(): Pair<Model, Effect<Message>> = Model(step = Model.Step.ProviderSelection) to none()

    fun makeUpdate(slack: Slack, platform: Platform) = { message: Message, model: Model ->
        when (message) {
            is Message.InspectProvider -> handle(message, model, slack, platform)
            is Message.InitiateLogin -> handle(message, model, platform, slack)
            is Message.GotAuthScopeStatus -> handle(message, model, platform)
            is Message.GotLoginResult -> handle(message, model, platform)
            Message.DismissProvider -> handleDismissProvider(model, slack)
        }
    }

    private fun handleDismissProvider(
        model: Model,
        slack: Slack
    ): Pair<Model, suspend CoroutineScope.(Dispatch<Message>) -> Any?> =
        model.copy(
            step = Model.Step.ProviderSelection
        ) to effect {
            slack.tearDownLogin()
        }

    private fun handle(
        message: Message.GotLoginResult,
        model: Model,
        platform: Platform
    ): Pair<Model, suspend CoroutineScope.(Dispatch<Message>) -> Any?> =
        model to effect {
            platform.logi { "Got login result: ${message.tokenResult}" }
        }

    private fun handle(
        message: Message.InspectProvider,
        model: Model,
        slack: Slack,
        platform: Platform
    ) = model.copy(
        step = Model.Step.ProviderInspection(provider = message.provider)
    ) to if (message.provider is Model.Provider.Slack) {
        effect { dispatch ->
            platform.logi { "Got message: $message" }
            slack.requestAuthScopes().collect { status ->
                dispatch(Message.GotAuthScopeStatus(status))
            }
        }
    } else {
        {
            TODO("Sign in to other providers")
        }
    }

    private fun handle(
        message: Message.InitiateLogin,
        model: Model,
        platform: Platform,
        slack: Slack
    ) = model to when (message.provider) {
        is Model.Provider.Slack -> effect<Message> { _ ->
            val clientId = "961165435895.5012210604118"
            val status = message.provider.status
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
        message: Message.GotAuthScopeStatus,
        model: Model,
        platform: Platform
    ): Pair<Model, suspend CoroutineScope.(Dispatch<Message>) -> Any?> {

        val logEffect = effect<Message> {
            platform.logi { "Got login status: $message" }
        }
        val exchangeEffect = { code: String ->
            effect<Message> {
                TODO("Exchange authorization code $code for access token")
            }
        }

        return when (message.status) {

            is AuthStatus.Route.Started,
            is AuthStatus.Route.Exposed
            -> model.copy(step = model.step.let { step ->
                check(step is Model.Step.ProviderInspection)
                check(step.provider is Model.Provider.Slack)
                step.copy(step.provider.copy(message.status))
            }) to logEffect

            is AuthStatus.Route.Init,
            is AuthStatus.Failure -> model to logEffect

            is AuthStatus.Success -> model to exchangeEffect(message.status.code)
        }
    }

    fun view(model: Model, dispatch: (Message) -> Unit) =
        Props(
            dialog = model.step
                .let { it as? Model.Step.ProviderInspection }
                ?.let { inspectionStep ->
                    val provider = inspectionStep.provider
                    val isButtonEnabled =
                        provider is Model.Provider.Slack &&
                                provider.status is AuthStatus.Route.Exposed
                    Props.Dialog.of(
                        button = Props.Button("Log in...") {
                            dispatch(Message.InitiateLogin(provider))
                        },
                        isButtonEnabled = isButtonEnabled,
                        onClose = { dispatch(Message.DismissProvider) },
                        provider.description
                    )
                },
            buttons = listOf(
                Props.Button("Slack") { dispatch(Message.InspectProvider(Model.Provider.Slack(AuthStatus.Route.Init))) },
                Props.Button("Jira") { dispatch(Message.InspectProvider(Model.Provider.Jira)) },
                Props.Button("GitHub") { dispatch(Message.InspectProvider(Model.Provider.GitHub)) },
            )
        )

    sealed interface Message {
        data class InspectProvider(val provider: Model.Provider) : Message
        data class InitiateLogin(val provider: Model.Provider) : Message
        data class GotAuthScopeStatus(val status: AuthStatus) : Message
        data class GotLoginResult(val tokenResult: Result<String>) : Message
        object DismissProvider : Message
    }

    data class Model(val step: Step) {
        sealed interface Step {
            object ProviderSelection : Step
            data class ProviderInspection(val provider: Provider) : Step
        }

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
