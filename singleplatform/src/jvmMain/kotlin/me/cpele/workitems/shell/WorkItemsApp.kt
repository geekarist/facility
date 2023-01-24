package me.cpele.workitems.shell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Card
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
                val items = props?.items
                if (items.isNullOrEmpty()) {
                    Text("You're all done! Good job.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        itemsIndexed(items) { _, item ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row {
                                        Text(item.title)
                                        Text(item.status)
                                    }
                                    Text(item.desc)
                                }
                            }
                        }
                    }
                }
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
