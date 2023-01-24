package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import oolong.Dispatch
import oolong.effect
import oolong.effect.none

object WorkItems {
    fun init() = Model(items = listOf(), status = Model.Status.Loading) to
            effect { dispatch ->
                dispatch(Event.LoadingStarted)
                delay(2000)
                val newItems =
                    listOf(
                        Model.Item(
                            title = "First loaded item",
                            desc = "First loaded item description",
                            status = Model.Item.Status.ToDo
                        ),
                        Model.Item(
                            title = "Second loaded item",
                            desc = "Second loaded item description",
                            status = Model.Item.Status.ToDo
                        ),
                        Model.Item(
                            title = "Third loaded item",
                            desc = "Third loaded item description",
                            status = Model.Item.Status.InProgress
                        ),
                        Model.Item(
                            title = "Fourth loaded item",
                            desc = "Fourth loaded item description",
                            status = Model.Item.Status.Done
                        )
                    )
                dispatch(Event.ItemsLoaded(newItems))
            }

    fun update(
        event: Event,
        model: Model
    ): Pair<Model, suspend CoroutineScope.(Dispatch<Event>) -> Any?> = when (event) {
        Event.LoadingStarted -> model.copy(status = Model.Status.Loading) to none()
        is Event.ItemsLoaded -> model.copy(status = Model.Status.Success, items = event.items) to none()
    }

    fun view(model: Model, function: (Event) -> Unit) = Props()

    data class Model(val status: Status, val items: List<Item>) {
        data class Item(val title: String, val desc: String, val status: Status) {
            enum class Status {
                ToDo, InProgress, Done
            }
        }

        sealed class Status {
            object Loading : Status()
            data class Failure(val t: Throwable) : Status()
            object Success : Status()
        }
    }

    sealed class Event {
        object LoadingStarted : Event()
        class ItemsLoaded(val items: List<Model.Item>) : Event()
    }

    data class Props(val items: List<Item> = emptyList()) {

        data class Item(val title: String, val desc: String, val status: String)
    }
}
