package me.cpele.workitems.core

import oolong.Effect

/**
 * Shorthand for a [Pair] of a new model and an effect dispatching events.
 *
 * Usually returned by program event handlers.
 *
 * @param ModelT The type of the new model
 * @param EventT The type of events dispatched by the [Effect]
 */
typealias Change<ModelT, EventT> = Pair<ModelT, Effect<EventT>>