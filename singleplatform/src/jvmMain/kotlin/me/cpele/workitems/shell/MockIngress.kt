package me.cpele.workitems.shell

object MockIngress : Ingress {
    override fun open(protocol: String, port: String, onTunnelOpened: (Ingress.Tunnel) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun close(tunnel: Ingress.Tunnel?) {
        TODO("Not yet implemented")
    }
}
