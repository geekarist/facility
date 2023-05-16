package me.cpele.workitems.shell

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
    Text("Yo")
}
