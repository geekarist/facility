package me.cpele.workitems.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object NgrokIngress : Ingress {

    private var processByTunnel = mapOf<Ingress.Tunnel, Process>()

    override suspend fun open(protocol: String, port: String, block: suspend (Ingress.Tunnel) -> Unit) {
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder("ngrok", protocol, port).start()
        }
        process.inputStream.bufferedReader().useLines { lineSeq ->
            lineSeq.forEach { line ->
                val jsonObj: Map<String, String> = deserializeJson(line)
                if (jsonObj["obj"] == "tunnels") {
                    block(Ingress.Tunnel(URL(jsonObj["url"])))
                }
            }
        }
    }

    private fun deserializeJson(line: String): Map<String, String> {
        TODO("Not yet implemented")
    }

    override fun close(tunnel: Ingress.Tunnel?) {
        val process = processByTunnel.getOrDefault(tunnel, null)
        process?.destroy()
        tunnel?.let {
            processByTunnel = processByTunnel.minus(tunnel)
        }
    }
}
