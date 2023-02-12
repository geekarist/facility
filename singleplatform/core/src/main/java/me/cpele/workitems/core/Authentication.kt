package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import oolong.Effect
import oolong.effect.none

object Authentication {
    fun init(): Pair<Model, Effect<Message>> = Model(step = Model.Step.ChooseProvider) to none()

    fun update(
        message: Message, model: Model
    ): Pair<Model, suspend CoroutineScope.((Message) -> Unit) -> Any?> = when (message) {
        Message.ProviderSelected -> model.copy(step = Model.Step.DetailProvider) to none()
    }

    fun view(model: Model, dispatch: (Message) -> Unit) = Props(
        Props.Dialog(isOpen = model.step == Model.Step.DetailProvider, text = "Yo", button = Props.Button("Yo") {}),
        listOf(
            Props.Button("Slack") { dispatch(Message.ProviderSelected) },
            Props.Button("Jira") {},
            Props.Button("GitHub") {},
        )
    )

    sealed interface Message {
        object ProviderSelected : Message
    }

    data class Model(val step: Step) {
        enum class Step {
            ChooseProvider,
            DetailProvider
        }
    }

    data class Props(
        val dialog: Dialog,
        val buttons: List<Button>
    ) {
        data class Dialog(val isOpen: Boolean, val text: String, val button: Button)

        data class Button(val text: String, val onClick: () -> Unit)
    }
}
