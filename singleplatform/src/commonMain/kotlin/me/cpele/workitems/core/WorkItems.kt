package me.cpele.workitems.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import oolong.Effect
import oolong.effect
import oolong.effect.none
import java.awt.Desktop
import java.net.URI

interface Slack {
    fun fetchMessages(): Result<List<Message>>

    interface Message {
        val text: String
    }
}

object WorkItems {
    fun makeInit(slack: Slack): () -> Pair<Model, Effect<Event>> = {
        Model(items = listOf(), status = Model.Status.Loading) to effect { dispatch ->
            dispatch(Event.SlackMessagesFetched(slack.fetchMessages()))
        }
    }

    fun update(
        event: Event, model: Model
    ): Pair<Model, Effect<Event>> = when (event) {

        is Event.SlackMessagesFetched -> {
            event.result.map {
                val items = it.map { message ->
                    Model.Item(
                        title = "Unknown title",
                        desc = message.text,
                        status = Model.Item.Status.ToDo,
                        url = "TODO"
                    )
                }
                model.copy(status = Model.Status.Success, items = items)
            }.getOrElse { _ ->
                model.copy(status = Model.Status.Failure, items = emptyList())
            } to none()
        }

        is Event.ItemClicked -> model to effect { _ ->
            println("Opening item: ${event.itemModel}...")
            withContext(Dispatchers.IO) {
                Desktop.getDesktop().browse(URI.create(event.itemModel.url))
            } // TODO: move to Platform interface?
        }
    }

    fun view(model: Model, dispatch: (Event) -> Unit) = when (model.status) {

        Model.Status.Loading -> Props(status = "⏳")

        Model.Status.Success -> Props(status = "✅", items = model.items.map { itemModel ->
            Props.Item(title = itemModel.title,
                desc = itemModel.desc,
                status = itemModel.status.text,
                onClick = { dispatch(Event.ItemClicked(itemModel)) })
        })

        Model.Status.Failure -> Props(status = "⚠")
    }

    data class Model(val status: Status, val items: List<Item>) {
        data class Item(val title: String, val desc: String, val status: Status, val url: String) {
            enum class Status(val text: String) {
                ToDo(text = "To do"), InProgress("In progress"), Done("Done")
            }
        }

        sealed class Status {
            object Loading : Status()
            object Success : Status()
            object Failure : Status()
        }
    }

    sealed class Event {
        data class SlackMessagesFetched(val result: Result<List<Slack.Message>>) : Event()
        data class ItemClicked(val itemModel: Model.Item) : Event()
    }

    data class Props(val status: String = "✅", val items: List<Item> = emptyList()) {
        data class Item(val title: String, val desc: String, val status: String, val onClick: () -> Unit)
    }
}
