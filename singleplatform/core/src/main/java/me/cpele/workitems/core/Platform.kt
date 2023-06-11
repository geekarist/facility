package me.cpele.workitems.core

interface Platform {
    fun logw(thrown: Throwable? = null, makeMessage: () -> String)
    fun logi(thrown: Throwable? = null, makeMessage: () -> String)
    fun openUri(url: String)
    fun fetch(url: String): Result<ByteArray>
}