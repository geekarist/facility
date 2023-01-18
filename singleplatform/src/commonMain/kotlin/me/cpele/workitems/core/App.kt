package me.cpele.workitems.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import oolong.Dispatch
import oolong.effect
import oolong.effect.none
import java.net.URL

object App {
    fun init() = Model("Yeah") to none<Event>()

    fun update(event: Event, model: Model) = when (event) {

        is Event.ButtonClicked -> model.copy(text = "Hi! Please hold...") to
                effect { dispatch ->
                    val url = URL("http://localhost:8000/hello")
                    val helloEvent = withContext(Dispatchers.IO) {
                        try {
                            val helloBody = url.openStream().use { inputStream ->
                                inputStream.readAllBytes().decodeToString().trim()
                            }
                            Event.GotHello(helloBody)
                        } catch (e: Exception) {
                            Event.Failure("Sorry, we're unavailable... Hello anyway.", e)
                        }
                    }
                    dispatch(helloEvent)
                }

        is Event.GotHello -> model.copy(text = event.helloBody) to none()
        is Event.Failure -> model.copy(text = event.msg) to effect { _: Dispatch<Event> ->
            println("Got failed greeting: $event")
            event.throwable?.printStackTrace()
        }
    }

    sealed class Event {
        object ButtonClicked : Event()
        data class GotHello(val helloBody: String) : Event()
        data class Failure(val msg: String, val throwable: Throwable? = null) : Event()
    }

    fun view(model: Model, dispatch: Dispatch<Event>): Props = Props(model.text, dispatch)

    data class Model(val text: String)

    data class Props(val text: String, val dispatch: Dispatch<Event>)
}