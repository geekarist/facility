package me.cpele.workitems.shell

import me.cpele.workitems.core.Authentication

fun main(vararg args: String) =
    if (args.contains("--mock")) {
        DesktopPlatform.logi { "Command launched with `--mock` argument ⇒ mocking Slack, Ingress effects" }
        Authentication.makeApp(
            DesktopPlatform,
            MockSlack
        )
    } else {
        DesktopPlatform.logi { "Command launched normally ⇒ default Slack, Ingress effects" }
        Authentication.makeApp(
            DesktopPlatform,
            DefaultSlack(
                DesktopPlatform,
                NgrokIngress
            )
        )
    }


