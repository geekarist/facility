package me.cpele.workitems.core.framework.effects

interface Preferences {
    suspend fun putString(key: String, value: String)
    suspend fun getString(key: String): String?
}
