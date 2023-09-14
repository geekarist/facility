package me.cpele.facility.core.programs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.cpele.facility.core.framework.Change
import me.cpele.facility.core.framework.effects.Platform
import me.cpele.facility.core.framework.effects.Slack
import oolong.effect
import oolong.effect.none

object WorkItems {
    fun makeInit(slack: Slack): () -> Change<Model, Event> = {
        Change(model = Model(
            items = listOf(), status = Model.Status.Loading
        ), effect = effect { dispatch ->
            dispatch(Event.SlackMessagesFetched(slack.fetchMessages()))
        })
    }

    fun makeUpdate(platform: Platform): (Event, Model) -> Change<Model, Event> = { event, model ->
        when (event) {
            is Event.SlackMessagesFetched -> {
                event.result.map { messages ->
                    val items = messages.map { message ->
                        Model.Item(
                            title = "Unknown title", desc = message.text, status = Model.Item.Status.ToDo, url = "TODO"
                        )
                    }
                    Change(model.copy(status = Model.Status.Success, items = items), none<Event>())
                }.getOrElse { thrown: Throwable ->
                    Change(model.copy(
                        status = Model.Status.Failure
                    ), effect {
                        platform.logw(thrown) { "Error fetching Slack messages" }
                    })
                }
            }

            is Event.ItemClicked -> Change(model, effect { _ ->
                withContext(Dispatchers.IO) {
                    platform.openUri(event.itemModel.url)
                }
            })
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

    data class Props(
        val status: String = "✅",
        val items: List<Item> = emptyList(),
    ) {
        data class Item(val title: String, val desc: String, val status: String, val onClick: () -> Unit)
    }
}
