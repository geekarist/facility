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
data class Change<ModelT, EventT>(val model: ModelT, private val effect: Effect<EventT>)