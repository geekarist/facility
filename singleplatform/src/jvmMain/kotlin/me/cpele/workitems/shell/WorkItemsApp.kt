package me.cpele.workitems.shell

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.cpele.workitems.core.WorkItems
import oolong.runtime

object WorkItemsApp {
    @Composable
    fun Ui(props: WorkItems.Props?) {
        TODO()
    }

    fun run() = application {
        var workItemsProps by rememberSaveable {
            mutableStateOf<WorkItems.Props?>(null)
        }
        Window(onCloseRequest = ::exitApplication) {
            Ui(workItemsProps)
        }
        LaunchedEffect(Unit) {
            runtime(
                init = WorkItems::init,
                update = WorkItems::update,
                view = WorkItems::view,
                render = {
                    it.also { workItemsProps = it }
                }
            )
        }
    }

}
