package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import oolong.Dispatch

object SlackSearch : UiProgram<SlackSearch.Model, SlackSearch.Event, SlackSearch.Props> {
    data class Model(val query: String = "", val result: Outcome? = null) {
        sealed class Outcome {
            object Loading : Outcome()
            data class Failure(val throwable: Throwable) : Outcome()
            data class Success(val messages: List<Message>) : Outcome()

            data class Message(val id: String, val text: String)
        }
    }

    class Event
    class Props

    override fun init() = Model()

    override fun view(model: Model, dispatch: Dispatch<Event>) = TODO("Not yet implemented")

    override fun update(
        model: Model,
        event: Event
    ): Pair<Model, suspend CoroutineScope.(Dispatch<Event>) -> Any?> = TODO("Not yet implemented")
}