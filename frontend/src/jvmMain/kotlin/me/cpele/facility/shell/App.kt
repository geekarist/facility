package me.cpele.facility.shell

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.application
import me.cpele.facility.core.framework.Change
import oolong.runtime

fun <PropsT, ModelT, EventT> app(
    init: () -> Change<ModelT, EventT>,
    update: (EventT, ModelT) -> Change<ModelT, EventT>,
    view: (ModelT, (EventT) -> Unit) -> PropsT,
    setOnQuitListener: (() -> Unit) -> Unit = {},
    ui: @Composable (PropsT) -> Unit
) {
    application {
        var props: PropsT by rememberSaveable {
            val initModel = init().model
            val initProps = view(initModel) {}
            mutableStateOf(initProps)
        }
        val coroutineScope = rememberCoroutineScope()
        setOnQuitListener(::exitApplication)
        ui(props)
        LaunchedEffect(Unit) {
            runtime(
                init = {
                    init().let { initialChange ->
                        initialChange.model to initialChange.effect
                    }
                },
                update = { event, model ->
                    update(event, model).let { change ->
                        change.model to change.effect
                    }
                },
                view = view,
                render = { newProps ->
                    newProps.also { props = it }
                },
                renderContext = coroutineScope.coroutineContext
            )
        }
    }
}