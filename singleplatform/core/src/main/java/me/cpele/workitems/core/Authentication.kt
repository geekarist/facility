package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import oolong.Effect
import oolong.effect.none

object Authentication {
    fun init(): Pair<Model, Effect<Message>> = Model(step = Model.Step.ProviderSelection) to none()

    fun update(
        message: Message, model: Model
    ): Pair<Model, suspend CoroutineScope.((Message) -> Unit) -> Any?> = when (message) {
        Message.InspectProvider -> model.copy(step = Model.Step.ProviderInspection) to none()
        Message.DismissProvider -> model.copy(step = Model.Step.ProviderSelection) to none()
    }

    fun view(model: Model, dispatch: (Message) -> Unit) = Props(
        Props.Dialog(
            isOpen = model.step == Model.Step.ProviderInspection,
            text = "Yo",
            button = Props.Button("Yo") {},
            onClose = { dispatch(Message.DismissProvider) }
        ),
        listOf(
            Props.Button("Slack") { dispatch(Message.InspectProvider) },
            Props.Button("Jira") {},
            Props.Button("GitHub") {},
        )
    )

    sealed interface Message {
        object InspectProvider : Message
        object DismissProvider : Message
    }

    data class Model(val step: Step) {
        enum class Step {
            ProviderSelection,
            ProviderInspection
        }
    }

    data class Props(
        val dialog: Dialog,
        val buttons: List<Button>
    ) {
        data class Dialog(val isOpen: Boolean, val text: String, val button: Button, val onClose: () -> Unit)

        data class Button(val text: String, val onClick: () -> Unit)
    }
}
