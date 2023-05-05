package me.cpele.workitems.shell

import me.cpele.workitems.core.Authentication

fun main(vararg args: String): Unit = Authentication.makeApp(
    DesktopPlatform,
    DefaultSlack(
        DesktopPlatform,
        NgrokIngress
    )
)

