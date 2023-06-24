package me.cpele.workitems.core.programs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.cpele.workitems.core.framework.Change
import me.cpele.workitems.core.framework.Platform
import me.cpele.workitems.core.framework.Prop
import me.cpele.workitems.core.framework.Prop.of
import me.cpele.workitems.core.framework.Slack
import me.cpele.workitems.core.framework.Slack.AuthenticationScopeStatus
import oolong.effect
import oolong.effect.none
import java.net.URLEncoder
import java.nio.charset.Charset

private const val SLACK_CLIENT_ID = "961165435895.5012210604118"

/**
 * This program implements user accounts and related functions, including:
 * - Credentials, personal info
 * - Authentication, inspection
 */
object Accounts {
    fun init(): Change<Model, Event> = Change(Model.ProviderSelection, none())

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
    ): Change<Model, Event> = Change(
        Model.ProviderSelection,
        effect {
            slack.tearDownLogin()
        })

    private fun handle(
        event: Event.GotLoginResult,
        model: Model,
        platform: Platform
    ): Change<Model, Event> = Change(model, effect {
        platform.logi { "Got login result: ${event.tokenResult}" }
    })

    private fun handle(
        event: Event.InspectProvider,
        slack: Slack,
        platform: Platform
    ): Change<Model, Event> = Change(
        Model.ProviderInspection(provider = event.provider),
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
    )

    private fun handle(
        event: Event.InitiateLogin,
        model: Model,
        platform: Platform,
        slack: Slack
    ) = Change(model, when (event.provider) {
        is Model.Provider.Slack -> effect<Event> { _ ->
            val status = event.provider.status
            check(status is AuthenticationScopeStatus.Route.Exposed) {
                val simpleName = AuthenticationScopeStatus.Route.Exposed::class.simpleName
                "Status should be $simpleName but is $status"
            }
            val decodedRedirectUri = status.url.toExternalForm()
            val redirectUri = withContext(Dispatchers.IO) {
                val charset = Charset.defaultCharset().name()
                URLEncoder.encode(decodedRedirectUri, charset)
            }
            val authUrl = slack.authUrlStr
            val scope = "incoming-webhook,commands"
            val url = "$authUrl?scope=$scope&client_id=$SLACK_CLIENT_ID&redirect_uri=$redirectUri"
            platform.openUri(url = url)
        }

        Model.Provider.Jira -> TODO()
        Model.Provider.GitHub -> TODO()
    })

    private fun handle(
        event: Event.GotAuthScopeStatus,
        model: Model,
        platform: Platform,
        slack: Slack
    ): Change<Model, Event> {

        val logEffect = effect<Event> {
            platform.logi { "Got login status: $event" }
        }
        val exchangeEffect = { code: String, redirectUri: String ->
            effect<Event> { dispatch ->
                val accessToken = slack.exchangeCodeForToken(code, SLACK_CLIENT_ID, "TODO", redirectUri)
                dispatch(Event.GotAccessToken(accessToken))
            }
        }

        return when (event.status) {

            is AuthenticationScopeStatus.Route.Started,
            is AuthenticationScopeStatus.Route.Exposed
            -> Change(
                model.let {
                    check(it is Model.ProviderInspection)
                    it
                }.let { providerInspectionModel ->
                    check(providerInspectionModel.provider is Model.Provider.Slack)
                    val provider = providerInspectionModel.provider.copy(event.status)
                    providerInspectionModel.copy(provider = provider)
                },
                logEffect
            )

            is AuthenticationScopeStatus.Route.Init,
            is AuthenticationScopeStatus.Failure -> Change(model, logEffect)

            is AuthenticationScopeStatus.Success -> Change(
                model,
                exchangeEffect(event.status.code, "todo-redirect-uri")
            )
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
                                provider.status is AuthenticationScopeStatus.Route.Exposed
                    Prop.Dialog.of(
                        button = Prop.Button("Log in...") {
                            dispatch(Event.InitiateLogin(provider))
                        },
                        isButtonEnabled = isButtonEnabled,
                        onClose = { dispatch(Event.DismissProvider) },
                        provider.description
                    )
                },
            buttons = listOf(
                Prop.Button("Slack") { dispatch(Event.InspectProvider(Model.Provider.Slack(AuthenticationScopeStatus.Route.Init))) },
                Prop.Button("Jira") { dispatch(Event.InspectProvider(Model.Provider.Jira)) },
                Prop.Button("GitHub") { dispatch(Event.InspectProvider(Model.Provider.GitHub)) },
            )
        )

    sealed interface Event {
        data class InspectProvider(val provider: Model.Provider) : Event
        data class InitiateLogin(val provider: Model.Provider) : Event
        data class GotAuthScopeStatus(val status: AuthenticationScopeStatus) : Event
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
            data class Slack(val status: AuthenticationScopeStatus) : Provider(
                description = "Slack lets you use reactions to tag certain messages, turning them into work items",
            )

            object Jira : Provider(description = "Jira tickets assigned to you appear as work items")
            object GitHub : Provider(description = "GitHub issues or PRs assigned to you appear as work items")
        }
    }

    data class Props(val dialog: Prop.Dialog? = null, val buttons: List<Prop.Button>)
}
