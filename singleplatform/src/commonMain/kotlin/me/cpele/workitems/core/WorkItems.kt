package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import oolong.Dispatch
import oolong.effect.none

object WorkItems {
    fun init() = Model(
        items = listOf(
            Model.Item(
                title = "First item",
                desc = "First item description",
                status = Model.Item.Status.ToDo,
            )
        )
    ) to none<Event>()

    fun update(
        event: Event,
        model: Model
    ): Pair<Model, suspend CoroutineScope.(Dispatch<Event>) -> Any?> {
        TODO("Not yet implemented")
    }

    fun view(model: Model, function: (Event) -> Unit) = Props()

    class Model(val items: List<Item>) {
        class Item(val title: String, val desc: String, val status: Status) {
            enum class Status {
                ToDo, InProgress, Done
            }
        }
    }

    class Event
    class Props

}
