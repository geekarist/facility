package me.cpele.workitems.shell

import me.cpele.workitems.core.Authentication

fun main(vararg args: String) =
    if (args.contains("--mock")) {
        Authentication.makeApp(
            DesktopPlatform,
            MockSlack(
                DesktopPlatform,
                MockIngress
            )
        )
    } else {
        Authentication.makeApp(
            DesktopPlatform,
            DefaultSlack(
                DesktopPlatform,
                NgrokIngress
            )
        )
    }


