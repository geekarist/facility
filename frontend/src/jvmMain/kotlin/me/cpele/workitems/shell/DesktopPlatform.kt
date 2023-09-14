package me.cpele.workitems.shell

import me.cpele.workitems.core.framework.effects.Platform
import java.awt.Desktop
import java.net.URI
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.min
import kotlin.system.exitProcess

object DesktopPlatform : Platform {
    private const val MAX_MSG_SIZE = 1024

    override fun logw(thrown: Throwable?, makeMessage: () -> String) {
        val level = Level.WARNING
        log(makeMessage, level, thrown)
    }

    override fun logi(thrown: Throwable?, makeMessage: () -> String) {
        val level = Level.INFO
        log(makeMessage, level, thrown)
    }

    private inline fun log(crossinline makeMessage: () -> String, level: Level?, thrown: Throwable?) {
        val makeShorterMessage = {
            val message = makeMessage()
            val maxIdx = min(message.length - 1, MAX_MSG_SIZE)
            message.substring(0..maxIdx)
        }
        Logger.getAnonymousLogger().log(level, thrown, makeShorterMessage)
    }

    override fun openUri(url: String) {
        logi { "Opening URI: ${url}..." }
        Desktop.getDesktop().browse(URI.create(url))
    }

    override fun fetch(url: String): Result<ByteArray> = Result.runCatching {
        URL(url).readBytes()
    }

    override suspend fun getEnvVar(name: String): Result<String> = Result.runCatching {
        System.getenv(name) ?: error("Required environment variable $name is not defined")
    }

    override suspend fun exit(status: Int) {
        exitProcess(status)
    }
}
