package me.cpele.workitems.shell

import me.cpele.workitems.core.framework.effects.Preferences
import java.util.prefs.Preferences as JavaPrefs

object DesktopPreferences : Preferences {
    override suspend fun putString(key: String, value: String) {
        val node = JavaPrefs.userNodeForPackage(javaClass)
        node.put(key, value)
    }

    override suspend fun getString(key: String): String? = run {
        val node = JavaPrefs.userNodeForPackage(javaClass)
        node.get(key, null)
    }
}
