package me.cpele.workitems.shell

import java.net.URL

interface Ingress {
    fun open(protocol: String, port: String): Tunnel
    fun close(tunnel: Tunnel)

    interface Tunnel {
        val url: URL
    }

}
