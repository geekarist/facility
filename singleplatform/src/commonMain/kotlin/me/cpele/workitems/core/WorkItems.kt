package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import oolong.Dispatch
import oolong.effect.none

object WorkItems {
    fun init() = Model() to none<Event>()

    fun update(
        event: Event,
        model: Model
    ): Pair<Model, suspend CoroutineScope.(Dispatch<Event>) -> Any?> {
        TODO("Not yet implemented")
    }

    fun view(model: Model, function: (Event) -> Unit) = Props()

    class Model
    class Event
    class Props

}
