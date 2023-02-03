package me.cpele.workitems.core

interface Platform {
    fun logw(thrown: Throwable, makeMessage: () -> String)
    fun openUri(url: String)
}