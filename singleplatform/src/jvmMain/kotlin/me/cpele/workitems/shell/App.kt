package me.cpele.workitems.shell

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import oolong.Effect
import oolong.runtime

fun <PropsT, ModelT, EventT> app(
    init: () -> Pair<ModelT, Effect<EventT>>,
    update: (EventT, ModelT) -> Pair<ModelT, Effect<EventT>>,
    view: (ModelT, (EventT) -> Unit) -> PropsT,
    ui: @Composable (PropsT) -> Unit
) {
    application {
        var props: PropsT by rememberSaveable {
            val initModel = init().first
            val initProps = view(initModel) {}
            mutableStateOf(initProps)
        }
        val coroutineScope = rememberCoroutineScope()
        Window(onCloseRequest = ::exitApplication) {
            ui(props)
        }
        LaunchedEffect(Unit) {
            runtime(
                init = init,
                update = update,
                view = view,
                render = {
                    it.also { props = it }
                }, renderContext = coroutineScope.coroutineContext
            )
        }
    }
}