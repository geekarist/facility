import kotlinx.coroutines.CoroutineScope
import oolong.Dispatch
import oolong.effect.none

object App {
    fun init() = Model("Yo") to none<Event>()

    fun update(event: Event, model: Model): Pair<Model, suspend CoroutineScope.(Dispatch<Event>) -> Any?> {
        println(event)
        return model to none()
    }

    class Event {

    }

    fun view(model: Model, dispatch: Dispatch<Event>): Props = Props(model.text, dispatch)

    data class Model(val text: String)

    data class Props(val text: String, val dispatch: Dispatch<Event>)
}