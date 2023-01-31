package com.nice.cxonechat.sample.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class manages storage of files which should be accessible via [TemporaryFileProvider].
 */
@Singleton
internal class TemporaryFileStorage(
    context: Context,
    private val baseDirectory: String?,
) {

    private val cacheDir: File by lazy { baseDirectory?.let(::File) ?: context.cacheDir }
    private val cacheFolder: File by lazy {
        val directory = File(cacheDir, "/tmp/")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        directory
    }

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, null)

    fun createFile(name: String): File? {
        val file = File(cacheFolder, name)
        val fileCreated = runCatching { file.createNewFile() }
        return if (fileCreated.isSuccess) file else null
    }

    fun clear() {
        cacheFolder.deleteRecursively()
        cacheFolder.mkdirs()
    }
}
