@file:Suppress("UnusedReceiverParameter")

package me.cpele.workitems.shell

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.cpele.workitems.core.Authentication

@Composable
fun Authentication.Ui(modifier: Modifier = Modifier, props: Authentication.Props) {
    Text(modifier = modifier, text = props.text)
}

fun Authentication.app() = app(
    Authentication.Props(),
    Authentication::init,
    Authentication::update,
    Authentication::view
) { Ui(props = it) }
