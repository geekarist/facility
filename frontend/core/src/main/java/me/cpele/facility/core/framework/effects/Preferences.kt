package me.cpele.facility.core.framework.effects

interface Preferences {
    suspend fun saveString(key: String, value: String)
    suspend fun loadString(key: String): String?
}
