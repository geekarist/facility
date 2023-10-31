package me.cpele.facility.shell

import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.cpele.facility.core.framework.effects.Platform
import java.net.URL
import kotlin.time.Duration.Companion.seconds

class NgrokIngress(private val platform: Platform) : Ingress {

    private var runningProcess: Process? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json {
        coerceInputValues = true
        isLenient = true
    }

    override fun open(protocol: String, port: String, onTunnelOpened: (Ingress.Tunnel) -> Unit) {
        check(runningProcess == null) {
            "Process already running: ${runningProcess?.pid()}"
        }
        coroutineScope.launch(Dispatchers.IO) {
            DesktopPlatform.logi { "Launching tunnel command..." }
            val command = listOf("ngrok", protocol, "--log=stdout", "--log-format=json", port)
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
        runningProcess?.let { process ->
            platform.logi { "Closing ingress" }
            runningProcess = null
            platform.logi { "Process to close: ${process.pid()}" }
            runBlocking {
                if (process.isAlive) {
                    platform.logi { "Process still alive ⇒ destroying" }
                    process.destroy()
                    delay(1.seconds)
                }
                if (process.isAlive) {
                    platform.logi { "Process still alive ⇒ waiting for 5 sec" }
                    delay(5.seconds)
                }
                if (process.isAlive) {
                    platform.logi { "Process still alive ⇒ destroying forcibly" }
                    process.destroyForcibly()
                }
                platform.logi { "Process destroyed ⇒ still alive? ${process.isAlive}" }
            }
        }
    }
}