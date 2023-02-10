@file:Suppress("UnusedReceiverParameter")

package me.cpele.workitems.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
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
    Dialog(visible = props.dialog.isOpen, onCloseRequest = {}) {
        Text(props.dialog.text)
        Button(onClick = props.dialog.button.onClick) {
            Text(props.dialog.button.text)
        }
    }
}

fun Authentication.app() = app(
    Authentication::init, Authentication::update, Authentication::view
) { Ui(props = it) }
