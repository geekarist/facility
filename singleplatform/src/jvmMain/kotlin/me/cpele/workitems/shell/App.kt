@file:Suppress("UnusedReceiverParameter")

package me.cpele.workitems.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.cpele.workitems.core.SignIn
import me.cpele.workitems.core.WorkItems
import oolong.runtime

@Composable
fun WorkItems.Ui(props: WorkItems.Props) {
    MaterialTheme {
        Box(Modifier.padding(16.dp).fillMaxSize()) {
            Text(modifier = Modifier.align(Alignment.TopEnd), text = props.status)
            SignIn.Ui(modifier = Modifier.align(Alignment.BottomEnd), props = props.signIn)

            val items = props.items
            if (items.isEmpty()) {
                Text("You're all done! Good job.")
            } else {
                LazyColumn(
                    modifier = Modifier.padding(8.dp),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(items) { _, item ->
                        Card(Modifier.fillMaxWidth().clickable {
                            item.onClick()
                        }) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row {
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = item.title,
                                        style = MaterialTheme.typography.h5
                                    )
                                    Text(text = item.status, style = MaterialTheme.typography.subtitle1)
                                }
                                Text(text = item.desc, style = MaterialTheme.typography.body1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignIn.Ui(modifier: Modifier = Modifier, props: SignIn.Props) {
    Text(modifier = modifier, text = props.text)
}

fun WorkItems.application() = application {
    var workItemsProps by rememberSaveable {
        mutableStateOf(WorkItems.Props())
    }
    val coroutineScope = rememberCoroutineScope()
    Window(onCloseRequest = ::exitApplication) {
        WorkItems.Ui(workItemsProps)
    }
    LaunchedEffect(Unit) {
        runtime(
            init = makeInit(DefaultSlack),
            update = makeUpdate(DesktopPlatform),
            view = WorkItems::view,
            render = {
                it.also { workItemsProps = it }
            }, renderContext = coroutineScope.coroutineContext
        )
    }
}
