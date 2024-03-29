package me.cpele.facility.shell

import java.net.URL

interface Ingress {
    fun open(protocol: String, port: String, onTunnelOpened: (Tunnel) -> Unit)
    fun close()

    data class Tunnel(val url: URL, val tag: Any? = null)
}
