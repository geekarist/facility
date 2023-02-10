@file:Suppress("UnusedReceiverParameter")

package me.cpele.workitems.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.cpele.workitems.core.Authentication

@Composable
fun Authentication.Ui(modifier: Modifier = Modifier, props: Authentication.Props) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp).wrapContentSize(unbounded = true),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = { props.onClickSlack() }) {
            Text("Slack")
        }
        Button(onClick = { props.onClickJira() }) {
            Text("Jira")
        }
        Button(onClick = { props.onClickGitHub() }) {
            Text("GitHub")
        }
    }
}

fun Authentication.app() = app(
    Authentication.Props(), Authentication::init, Authentication::update, Authentication::view
) { Ui(props = it) }
