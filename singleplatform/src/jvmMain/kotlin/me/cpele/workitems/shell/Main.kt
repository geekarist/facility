package me.cpele.workitems.shell

import me.cpele.workitems.core.Account

fun main(vararg args: String) =
    if (args.contains("--mock")) {
        DesktopPlatform.logi { "Command launched with `--mock` argument ⇒ mocking Slack, Ingress effects" }
        Account.makeApp(
            DesktopPlatform,
            MockSlack
        )
    } else {
        DesktopPlatform.logi { "Command launched normally ⇒ default Slack, Ingress effects" }
        Account.makeApp(
            DesktopPlatform,
            DefaultSlack(
                DesktopPlatform,
                NgrokIngress
            )
        )
    }


