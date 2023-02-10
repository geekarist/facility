package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import oolong.Effect
import oolong.effect
import oolong.effect.none
import java.util.logging.Level
import java.util.logging.Logger

object Authentication {
    fun init(): Pair<Model, Effect<Event>> = Model() to none()

    fun update(
        event: Event, model: Model
    ): Pair<Model, suspend CoroutineScope.((Event) -> Unit) -> Any?> = when (event) {
        else -> model to effect {
            Logger.getAnonymousLogger().log(Level.INFO, "Unknown event: $event")
        }
    }

    fun view(model: Model, dispatch: (Event) -> Unit) = Props(
        Props.Dialog(isOpen = false, text = "Yo", button = Props.Button("Yo") {}),
        listOf(
            Props.Button("Slack") {},
            Props.Button("Jira") {},
            Props.Button("GitHub") {},
        )
    )

    sealed interface Event {
    }

    class Model

    data class Props(
        val dialog: Dialog,
        val buttons: List<Button>
    ) {
        data class Dialog(val isOpen: Boolean, val text: String, val button: Button)

        data class Button(val text: String, val onClick: () -> Unit)
    }
}
