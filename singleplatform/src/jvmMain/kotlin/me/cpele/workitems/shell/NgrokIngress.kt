package me.cpele.workitems.shell

object NgrokIngress : Ingress {

    override fun open(protocol: String, port: String, block: suspend (Ingress.Tunnel) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun close(tunnel: Ingress.Tunnel?) {
        TODO("Not yet implemented")
    }
}
