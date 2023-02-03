package me.cpele.workitems.shell

import me.cpele.workitems.core.Platform
import java.awt.Desktop
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger

object DesktopPlatform : Platform {
    override fun logw(thrown: Throwable, makeMessage: () -> String) {
        val level = Level.WARNING
        val msg = makeMessage()
        Logger.getAnonymousLogger().log(level, msg, thrown)
    }

    override fun openUri(url: String) {
        println("Opening URI: ${url}...")
        Desktop.getDesktop().browse(URI.create(url))
    }
}
