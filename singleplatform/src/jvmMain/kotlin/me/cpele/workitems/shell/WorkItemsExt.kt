@file:Suppress("UnusedReceiverParameter")

package me.cpele.workitems.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.cpele.workitems.core.programs.WorkItems

@Composable
fun WorkItems.Ui(
    props: WorkItems.Props,
    @Suppress("UNUSED_PARAMETER")
    exitApp: () -> Unit = {} // TODO: Define Window in this composable
) {
    MaterialTheme {
        Box(Modifier.padding(16.dp).fillMaxSize()) {
            Text(modifier = Modifier.align(Alignment.TopEnd), text = props.status)

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

fun WorkItems.app() = app(
    init = makeInit(DefaultSlack(DesktopPlatform, NgrokIngress(DesktopPlatform))),
    update = makeUpdate(DesktopPlatform),
    view = WorkItems::view
) { props -> WorkItems.Ui(props) }

fun WorkItems.main(vararg args: String) = app()