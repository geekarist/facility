package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import oolong.Effect
import oolong.effect.none

object Authentication {
    fun init(): Pair<Model, Effect<Event>> = Model() to none()

    fun update(
        event: Event,
        model: Model
    ): Pair<Model, suspend CoroutineScope.((Event) -> Unit) -> Any?> = model to none()

    fun view(model: Model, function: (Event) -> Unit): Props = Props()

    class Event

    class Model

    class Props(val text: String = "Yo")
}
