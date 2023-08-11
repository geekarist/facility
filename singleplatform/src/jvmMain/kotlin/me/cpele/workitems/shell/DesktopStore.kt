package me.cpele.workitems.shell

import me.cpele.workitems.core.framework.effects.Store

object DesktopStore : Store {
    override suspend fun getString(key: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun putString(key: String, data: String) {
        TODO("Not yet implemented")
    }
}
