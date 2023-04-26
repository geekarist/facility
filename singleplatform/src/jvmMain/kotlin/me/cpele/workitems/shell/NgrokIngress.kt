package me.cpele.workitems.shell

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URL
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object NgrokIngress : Ingress {

    private var processByTunnel = mapOf<Ingress.Tunnel, Process>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val json = Json {
        coerceInputValues = true
    }

    override fun open(protocol: String, port: String, onTunnelOpened: (Ingress.Tunnel) -> Unit) {
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
            process.inputStream.bufferedReader().useLines { lineSeq ->
                lineSeq.mapNotNull { line ->
                    val jsonObj: Map<String, String> = deserializeJson(line)
                    if (jsonObj["obj"] == "tunnels") {
                        Ingress.Tunnel(URL(jsonObj["url"]), jsonObj)
                    } else {
                        null
                    }
                }.forEach { onTunnelOpened(it) }
            }
        }
    }

    private fun deserializeJson(line: String): Map<String, String> = json.decodeFromString(line)

    override fun close(tunnel: Ingress.Tunnel?) {
        val process = processByTunnel.getOrDefault(tunnel, null)
        coroutineScope.launch {
            process?.destroy() S
                    delay(30.seconds)
            process?.destroyForcibly()
        }
        tunnel?.let {
            processByTunnel = processByTunnel.minus(tunnel)
        }
    }
}

fun main() {
    NgrokIngress.open(protocol = "http", port = "8080") {
        DesktopPlatform.logi { "Opened tunnel: $it" }
    }
    Thread.sleep(10.minutes.inWholeMilliseconds)
}
