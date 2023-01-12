import kotlinx.coroutines.CoroutineScope
import oolong.Dispatch
import oolong.effect.none

object App {
    fun init() = Model("Yo") to none<Event>()

    fun update(
        @Suppress("UNUSED_PARAMETER") event: Event,
        model: Model
    ): Pair<Model, suspend CoroutineScope.(Dispatch<Event>) -> Any?> =
        model.copy(text = "Hello! Thanks for clicking \uD83D\uDE0C") to none()

    sealed class Event {
        object ButtonClicked : Event()
    }

    fun view(model: Model, dispatch: Dispatch<Event>): Props = Props(model.text, dispatch)

    data class Model(val text: String)

    data class Props(val text: String, val dispatch: Dispatch<Event>)
}