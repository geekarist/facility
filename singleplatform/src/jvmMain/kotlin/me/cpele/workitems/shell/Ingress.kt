package me.cpele.workitems.shell

import java.net.URL

interface Ingress {
    fun open(protocol: String, port: String, onTunnelOpened: (Tunnel) -> Unit)
    fun close(tunnel: Tunnel?)

    data class Tunnel(val url: URL)
}
