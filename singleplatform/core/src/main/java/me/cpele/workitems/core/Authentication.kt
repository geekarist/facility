package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import oolong.Effect

object Authentication {
    fun init(): Pair<Model, Effect<Event>> {
        TODO("Not yet implemented")
    }

    fun update(
        event: Event,
        model: Model
    ): Pair<Model, suspend CoroutineScope.((Event) -> Unit) -> Any?> {
        TODO("Not yet implemented")
    }

    fun view(model: Model, function: (Event) -> Unit): Props {
        TODO("Not yet implemented")
    }

    class Event {

    }

    class Model {

    }

    class Props(val text: String = "Yo")
}
