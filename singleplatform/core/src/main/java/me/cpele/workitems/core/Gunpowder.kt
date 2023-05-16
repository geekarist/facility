package me.cpele.workitems.core

import oolong.Effect

/**
 * Wrapper of a new model and an effect dispatching events.
 *
 * Usually returned by program event handlers.
 *
 * @param ModelT The type of the new model
 * @param EventT The type of events dispatched by the [Effect]
 *
 * @property model The new model
 * @property effect The effect to apply
 */
data class Change<ModelT, EventT>(val model: ModelT, val effect: Effect<EventT>)

data class Button(
    val text: String,
    val isEnabled: Boolean = true,
    val onClick: () -> Unit
)

data class Dialog(
    val texts: Collection<String>,
    val isButtonEnabled: Boolean,
    val button: Button,
    val onClose: () -> Unit
) {
    companion object
}

fun Dialog.Companion.of(
    button: Button,
    isButtonEnabled: Boolean,
    onClose: () -> Unit,
    vararg texts: String
): Dialog = Dialog(
    texts = texts.toList(),
    isButtonEnabled = isButtonEnabled,
    button = button,
    onClose = onClose
)
