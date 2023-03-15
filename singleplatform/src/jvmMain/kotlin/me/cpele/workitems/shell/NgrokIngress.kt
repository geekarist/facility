package me.cpele.workitems.shell

import kotlinx.coroutines.*
import java.net.URL
import kotlin.time.Duration.Companion.seconds

object NgrokIngress : Ingress {

    private var processByTunnel = mapOf<Ingress.Tunnel, Process>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun open(protocol: String, port: String, onTunnelOpened: (Ingress.Tunnel) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val process = ProcessBuilder("ngrok", protocol, port).start()
            process.inputStream.bufferedReader().useLines { lineSeq ->
                lineSeq.forEach { line ->
                    val jsonObj: Map<String, String> = deserializeJson(line)
                    if (jsonObj["obj"] == "tunnels") {
                        onTunnelOpened(Ingress.Tunnel(URL(jsonObj["url"])))
                    }
                }
            }
        }
    }

    private fun deserializeJson(line: String): Map<String, String> {
        TODO("Not yet implemented")
    }

    override fun close(tunnel: Ingress.Tunnel?) {
        val process = processByTunnel.getOrDefault(tunnel, null)
        coroutineScope.launch {
            process?.destroy()
            delay(30.seconds)
            process?.destroyForcibly()
        }
        tunnel?.let {
            processByTunnel = processByTunnel.minus(tunnel)
        }
    }
}
