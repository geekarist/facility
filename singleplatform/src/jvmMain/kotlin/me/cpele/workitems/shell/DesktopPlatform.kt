package me.cpele.workitems.shell

import me.cpele.workitems.core.Platform
import java.awt.Desktop
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger

object DesktopPlatform : Platform {
    override fun logw(thrown: Throwable?, makeMessage: () -> String) {
        val level = Level.WARNING
        log(makeMessage, level, thrown)
    }

    override fun logi(thrown: Throwable?, makeMessage: () -> String) {
        val level = Level.INFO
        log(makeMessage, level, thrown)
    }

    private inline fun log(makeMessage: () -> String, level: Level?, thrown: Throwable?) {
        val msg = makeMessage()
        Logger.getAnonymousLogger().log(level, msg, thrown)
    }

    override fun openUri(url: String) {
        logi { "Opening URI: ${url}..." }
        Desktop.getDesktop().browse(URI.create(url))
    }
}
