package me.cpele.workitems.shell

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.cpele.workitems.core.framework.effects.Platform
import java.net.URL
import kotlin.time.Duration.Companion.seconds

class NgrokIngress(private val platform: Platform) : Ingress {

    private var runningProcess: Process? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val json = Json {
        coerceInputValues = true
        isLenient = true
    }

    override fun open(protocol: String, port: String, onTunnelOpened: (Ingress.Tunnel) -> Unit) {
        check(runningProcess == null) {
            "Already-running process: ${runningProcess?.pid()}"
        }
        coroutineScope.launch(Dispatchers.IO) {
            DesktopPlatform.logi { "Launching tunnel command..." }
            val command = listOf("ngrok", "--log=stdout", "--log-format=json", protocol, port)
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            DesktopPlatform.logi {
                val commandStr = command.joinToString(" ")
                "Command is: $commandStr"
            }
            DesktopPlatform.logi { "Reading command output" }
            runningProcess = process
            process.inputStream.bufferedReader().useLines { lineSeq ->
                lineSeq.mapNotNull { line ->
                    DesktopPlatform.logi { "Got line: $line" }
                    val jsonObj: Map<String, String?> = deserializeJson(line)
                    if (jsonObj["obj"] == "tunnels") {
                        Ingress.Tunnel(URL(jsonObj["url"]), jsonObj)
                    } else {
                        null
                    }
                }.forEach { onTunnelOpened(it) }
            }
        }
    }

    private fun deserializeJson(line: String): Map<String, String?> = json.decodeFromString(line)

    override fun close() {
        val process = requireNotNull(runningProcess)
        platform.logi { "Closing ingress" }
        platform.logi { "Running process to close: ${process.pid()}" }
        coroutineScope.launch {
            platform.logi { "Destroying process..." }
            process.destroy()
            platform.logi { "Waiting for some time..." }
            delay(30.seconds)
            platform.logi { "Destroying process!" }
            process.destroyForcibly()
            platform.logi { "Process still alive? ${process.isAlive}" }
        }
    }
}