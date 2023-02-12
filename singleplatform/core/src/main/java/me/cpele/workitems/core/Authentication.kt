package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import oolong.Effect
import oolong.effect.none

object Authentication {
    fun init(): Pair<Model, Effect<Message>> = Model(step = Model.Step.ProviderSelection) to none()

    fun update(
        message: Message, model: Model
    ): Pair<Model, suspend CoroutineScope.((Message) -> Unit) -> Any?> = when (message) {
        is Message.InspectProvider -> model.copy(step = Model.Step.ProviderInspection(provider = message.provider)) to none()
        Message.DismissProvider -> model.copy(step = Model.Step.ProviderSelection) to none()
    }

    fun view(model: Model, dispatch: (Message) -> Unit) = Props(
        dialog = (model.step as? Model.Step.ProviderInspection)?.let { inspectionStep: Model.Step.ProviderInspection ->
                Props.Dialog(text = "TODO: Inspect provider: ${inspectionStep.provider}",
                    button = Props.Button("Yo") {},
                    onClose = { dispatch(Message.DismissProvider) })
            }, buttons = listOf(
            Props.Button("Slack") { dispatch(Message.InspectProvider(Model.Provider.Slack)) },
            Props.Button("Jira") { dispatch(Message.InspectProvider(Model.Provider.Jira)) },
            Props.Button("GitHub") { dispatch(Message.InspectProvider(Model.Provider.GitHub)) },
        )
    )

    sealed interface Message {
        data class InspectProvider(val provider: Model.Provider) : Message
        object DismissProvider : Message
    }

    data class Model(val step: Step) {
        sealed interface Step {
            object ProviderSelection : Step
            data class ProviderInspection(val provider: Provider) : Step
        }

        enum class Provider {
            Slack, Jira, GitHub
        }
    }

    data class Props(
        val dialog: Dialog? = null, val buttons: List<Button>
    ) {
        data class Dialog(val text: String, val button: Button, val onClose: () -> Unit)

        data class Button(val text: String, val onClick: () -> Unit)
    }
}
