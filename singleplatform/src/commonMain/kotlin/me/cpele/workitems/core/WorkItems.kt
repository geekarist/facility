package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import oolong.Dispatch
import oolong.effect
import oolong.effect.none
import java.awt.Desktop
import java.net.URI

interface Slack {
    fun fetchMessages(): List<Message>

    class Message
}

object WorkItems {
    fun getInit(slack: Slack): () -> Pair<Model, suspend CoroutineScope.((Event) -> Unit) -> Any?> = {
        Model(items = listOf(), status = Model.Status.Loading) to effect { dispatch ->
            dispatch(Event.LoadingStarted)
            val messages: List<Slack.Message> = slack.fetchMessages()
            dispatch(Event.SlackMessagesFetched(messages))
        }
    }

    fun update(
        event: Event,
        model: Model
    ): Pair<Model, suspend CoroutineScope.(Dispatch<Event>) -> Any?> = when (event) {
        Event.LoadingStarted -> model.copy(status = Model.Status.Loading) to none()
        is Event.SlackMessagesFetched -> TODO()
        is Event.ItemsLoaded -> model.copy(status = Model.Status.Success, items = event.items) to none()
        is Event.ItemClicked -> model to effect { _ ->
            println("Opening item: ${event.itemModel}...")
            withContext(Dispatchers.IO) {
                Desktop.getDesktop().browse(URI.create(event.itemModel.url))
            }
        }

    }

    fun view(model: Model, dispatch: (Event) -> Unit) = Props(items = model.items.map { itemModel ->
        Props.Item(title = itemModel.title,
            desc = itemModel.desc,
            status = itemModel.status.text,
            onClick = { dispatch(Event.ItemClicked(itemModel)) })
    })

    data class Model(val status: Status, val items: List<Item>) {
        data class Item(val title: String, val desc: String, val status: Status, val url: String) {
            enum class Status(val text: String) {
                ToDo(text = "To do"), InProgress("In progress"), Done("Done")
            }
        }

        sealed class Status {
            object Loading : Status()
            object Success : Status()
        }
    }

    sealed class Event {
        object LoadingStarted : Event()
        data class SlackMessagesFetched(val messages: List<Slack.Message>) : Event()
        data class ItemsLoaded(val items: List<Model.Item>) : Event()
        data class ItemClicked(val itemModel: Model.Item) : Event()
    }

    data class Props(val items: List<Item> = emptyList()) {
        data class Item(val title: String, val desc: String, val status: String, val onClick: () -> Unit)
    }
}
