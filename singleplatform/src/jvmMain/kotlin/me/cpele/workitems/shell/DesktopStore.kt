package me.cpele.workitems.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.sink
import kotlinx.io.files.source
import kotlinx.io.readString
import kotlinx.io.writeString
import me.cpele.workitems.core.framework.effects.Store
import net.harawata.appdirs.AppDirsFactory
import java.io.FileNotFoundException
import java.nio.file.Files
import java.util.*
import java.nio.file.Path as NioPath

object DesktopStore : Store {
    override suspend fun getString(key: String): String? = run {
        DesktopPlatform.logi { "Reading key: $key" }
        val appDirs = AppDirsFactory.getInstance()
        val userDataDirPath = requireNotNull(
            appDirs.getUserDataDir("Slaccount", "1.0.0", "cpele")
        )
        Files.createDirectories(NioPath.of(userDataDirPath))
        val storeFilePath = buildStoreFilePath(userDataDirPath, key)
        val storeFileSource = try {
            storeFilePath.source()
        } catch (e: FileNotFoundException) {
            DesktopPlatform.logi { "Store file not found ⇒ returning null value" }
            // This is expected e.g. on first launch
            null
        }
        withContext(Dispatchers.IO) {
            storeFileSource?.use {
                it.readString()
            }
        }
    }

    override suspend fun putString(key: String, data: String) = run {
        DesktopPlatform.logi { "Reading key: $key" }
        val appDirs = AppDirsFactory.getInstance()
        val userDataDirPath = requireNotNull(
            appDirs.getUserDataDir("Slaccount", "1.0.0", "cpele")
        )
        val storeFilePath = buildStoreFilePath(userDataDirPath, key)
        // TODO: Create parent directories
        DesktopPlatform.logi { "Writing to store: $storeFilePath" }
        val storeFileSink = storeFilePath.sink()
        withContext(Dispatchers.IO) {
            storeFileSink.use {
                it.writeString(data)
            }
        }
    }

    private fun buildStoreFilePath(userDataDirPath: String, key: String): Path {
        val fileName = UUID.nameUUIDFromBytes(key.toByteArray())
        val storePathStr = "$userDataDirPath/slaccount-model-$fileName"
        DesktopPlatform.logi { "Store file path: $storePathStr" }
        return Path(storePathStr)
    }
}
