@file:Suppress("UnusedReceiverParameter")

package me.cpele.workitems.shell

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.cpele.workitems.core.Authentication

@Composable
fun Authentication.Ui(modifier: Modifier = Modifier, props: Authentication.Props) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp).wrapContentSize(unbounded = true),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        props.buttons.forEach { button ->
            Button(onClick = button.onClick) {
                Text(button.text)
            }
        }
    }
    props.dialog?.let { dialog ->
        Dialog(onCloseRequest = { dialog.onClose() }) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dialog.texts.forEach { Text(it) }
                Button(onClick = dialog.button.onClick) {
                    Text(dialog.button.text)
                }
            }
        }
    }
}

fun Authentication.app() = app(
    ::init, makeUpdate(DefaultSlack), ::view
) { Ui(props = it) }
