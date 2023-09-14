package me.cpele.workitems.core.framework.effects

// TODO: Suspend functions
interface Platform {
    fun logw(thrown: Throwable? = null, makeMessage: () -> String)
    fun logi(thrown: Throwable? = null, makeMessage: () -> String)
    fun openUri(url: String)
    fun fetch(url: String): Result<ByteArray>
    suspend fun getEnvVar(name: String): Result<String>
    suspend fun exit(status: Int = 0)
}