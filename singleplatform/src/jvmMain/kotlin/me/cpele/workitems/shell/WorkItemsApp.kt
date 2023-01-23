package me.cpele.workitems.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.cpele.workitems.core.WorkItems
import oolong.runtime

object WorkItemsApp {
    @Composable
    fun Ui(props: WorkItems.Props?) {
        MaterialTheme {
            Box(Modifier.padding(32.dp)) {
                Text("Yo")
            }
        }
    }

    fun run() = application {
        var workItemsProps by rememberSaveable {
            mutableStateOf<WorkItems.Props?>(null)
        }
        val coroutineScope = rememberCoroutineScope()
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
                },
                renderContext = coroutineScope.coroutineContext
            )
        }
    }

}
