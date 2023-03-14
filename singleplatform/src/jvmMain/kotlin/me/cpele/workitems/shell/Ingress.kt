package me.cpele.workitems.shell

import java.net.URL

interface Ingress {
    suspend fun open(protocol: String, port: String, block: suspend (Tunnel) -> Unit)
    fun close(tunnel: Tunnel?)

    data class Tunnel(val url: URL)

}
