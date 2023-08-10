package me.cpele.workitems.core.programs

interface Session {
    suspend fun <T> store(data: T)
    suspend fun <T> retrieve(): T
}
