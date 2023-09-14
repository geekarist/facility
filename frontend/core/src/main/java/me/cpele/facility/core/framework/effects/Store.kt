package me.cpele.facility.core.framework.effects

interface Store {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, data: String)
}