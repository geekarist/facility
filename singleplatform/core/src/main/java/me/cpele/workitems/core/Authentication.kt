package me.cpele.workitems.core

import oolong.Effect
import oolong.effect
import oolong.effect.none
import java.util.logging.Level
import java.util.logging.Logger

/**
 * This program implements the authentication process.
 */
object Authentication {
    fun init(): Pair<Model, Effect<Message>> = Model(step = Model.Step.ProviderSelection) to none()

    fun makeUpdate(slack: Slack) = { message: Message, model: Model ->
        when (message) {
            is Message.InspectProvider -> model.copy(step = Model.Step.ProviderInspection(provider = message.provider)) to none()
            Message.DismissProvider -> model.copy(step = Model.Step.ProviderSelection) to none()
            is Message.InitiateLogin -> model to when (message.provider) {
                Model.Provider.Slack -> effect<Message> { dispatch ->
                    dispatch(Message.GotLoginResult(slack.logIn()))
                }
                Model.Provider.Jira -> TODO()
                Model.Provider.GitHub -> TODO()
            }

            is Message.GotLoginResult -> model to effect {
                Logger.getAnonymousLogger().log(Level.INFO, "Got login result: ${message.tokenResult}")
            }
        }
    }

    fun view(model: Model, dispatch: (Message) -> Unit) =
        Props(
            dialog = model.step
                .let { it as? Model.Step.ProviderInspection }
                ?.let { inspectionStep ->
                    Props.Dialog(text = inspectionStep.provider.description,
                        button = Props.Button("Log in") { dispatch(Message.InitiateLogin(inspectionStep.provider)) },
                        onClose = { dispatch(Message.DismissProvider) })
                },
            buttons = listOf(
                Props.Button("Slack") { dispatch(Message.InspectProvider(Model.Provider.Slack)) },
                Props.Button("Jira") { dispatch(Message.InspectProvider(Model.Provider.Jira)) },
                Props.Button("GitHub") { dispatch(Message.InspectProvider(Model.Provider.GitHub)) },
            )
        )

    sealed interface Message {
        data class InspectProvider(val provider: Model.Provider) : Message
        data class InitiateLogin(val provider: Model.Provider) : Message
        data class GotLoginResult(val tokenResult: Result<String>) : Message
        object DismissProvider : Message
    }

    data class Model(val step: Step) {
        sealed interface Step {
            object ProviderSelection : Step
            data class ProviderInspection(val provider: Provider) : Step
        }

        enum class Provider(
            val description: String
        ) {
            Slack(description = "Slack lets you use reactions to tag certain messages, turning them into work items"), Jira(
                description = "Jira tickets assigned to you appear as work items"
            ),
            GitHub(description = "GitHub issues or PRs assigned to you appear as work items");
        }
    }

    data class Props(
        val dialog: Dialog? = null, val buttons: List<Button>
    ) {
        data class Dialog(val text: String, val button: Button, val onClose: () -> Unit)

        data class Button(val text: String, val onClick: () -> Unit)
    }
}
