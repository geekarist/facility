package me.cpele.facility.core.framework.effects

interface AppRuntime {
    suspend fun exit()

    companion object {
        fun of(exit: () -> Unit): AppRuntime = object : AppRuntime {
            override suspend fun exit() = exit()
        }
    }
}


