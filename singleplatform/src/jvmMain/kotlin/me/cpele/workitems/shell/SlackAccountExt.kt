package me.cpele.workitems.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.cpele.workitems.core.Platform
import me.cpele.workitems.core.Slack
import me.cpele.workitems.core.SlackAccount

fun main(args: Array<String>) {
    if (args.contains("--mock")) {
        SlackAccount.makeApp(MockSlack, DesktopPlatform)
    } else {
        SlackAccount.makeApp(DefaultSlack(DesktopPlatform, NgrokIngress), DesktopPlatform)
    }
}

private fun SlackAccount.makeApp(slack: Slack, platform: Platform) {
    app(
        init = ::init,
        update = makeUpdate(slack, platform),
        view = ::view,
        ui = {
            Ui(it)
        }
    )
}

@Composable
private fun SlackAccount.Ui(props: SlackAccount.Props) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (props) {

            is SlackAccount.Props.SignedIn -> TODO()

            is SlackAccount.Props.SignedOut -> Button(
                onClick = props.button.onClick,
                enabled = props.button.isEnabled
            ) {
                Text(text = props.button.text)
            }

            is SlackAccount.Props.SigningIn -> TODO()
        }
    }
}
