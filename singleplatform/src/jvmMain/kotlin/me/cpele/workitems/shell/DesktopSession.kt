package me.cpele.workitems.shell

import me.cpele.workitems.core.programs.Session

object DesktopSession : Session {
    override suspend fun <T> store(data: T) {
        TODO("Not yet implemented")
    }

    override suspend fun <T> retrieve(): T {
        TODO("Not yet implemented")
    }
}