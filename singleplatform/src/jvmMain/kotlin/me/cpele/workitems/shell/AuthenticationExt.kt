@file:Suppress("UnusedReceiverParameter")

package me.cpele.workitems.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.cpele.workitems.core.Authentication

@Composable
fun Authentication.Ui(modifier: Modifier = Modifier, props: Authentication.Props) {
    Button(modifier = modifier.padding(8.dp), onClick = {}) {
        Text(modifier = modifier, text = props.text)
    }
}

fun Authentication.app() = app(
    Authentication.Props(),
    Authentication::init,
    Authentication::update,
    Authentication::view
) { Ui(props = it) }
