package com.nice.cxonechat.sample.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nice.cxonechat.sample.model.AttachmentMessage
import com.nice.cxonechat.sample.storage.TemporaryFileProvider
import com.nice.cxonechat.sample.storage.TemporaryFileStorage
import com.nice.cxonechat.sample.util.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class responsible for caching of attachments before they can be shared
 * and creation of [Intent] which will be used for sharing.
 */
@Singleton
internal class AttachmentSharingRepository @Inject constructor(
    private val storage: TemporaryFileStorage,
) {

    private val client by lazy { OkHttpClient() }

    /**
     * Caches file from the original url to storage and creates Intent with action [Intent.ACTION_SEND] set to provide
     * data via stream from [TemporaryFileProvider].
     *
     * @param message AttachmentMessage whose content will be cached to file and that file shared.
     * @param context Context which will be used for caching.
     * @return Prepared intent or null if caching has failed.
     */
    suspend fun createSharingIntent(
        message: AttachmentMessage,
        context: Context,
    ): Intent? = runCatching {
        val filename = message.text
        val file = cacheUrlContents(message.originalUrl, filename, context) ?: return null
        val uri = TemporaryFileProvider.getUriForFile(file, filename, context)
        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, "Share attachment $filename")
            putExtra(Intent.EXTRA_TEXT, filename)
            putExtra(Intent.EXTRA_STREAM, uri)
            setDataAndType(uri, message.mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }.getOrNull()

    private suspend fun cacheUrlContents(
        url: String,
        name: String,
        context: Context,
    ): File? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(url)
        val stream: InputStream = when (uri.scheme) {
            // Attachment can be available via content if the message was just sent, before it is updated from backend.
            "content" -> runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            "http", "https" -> {
                val request = Builder().url(url).build()
                runCatching { client.newCall(request).await() }.getOrNull()?.byteStream()
            }
            else -> null
        } ?: return@withContext null
        storeToCache(stream, name)
    }

    /**
     * Simple function which writes given [InputStream] to file which will be accessible from [TemporaryFileProvider].
     * If there is already a file for given [fileName], then it will be overwritten.
     */
    private suspend fun storeToCache(inputStream: InputStream, fileName: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            storage.createFile(fileName)?.also { file ->
                val sink = file.outputStream().sink().buffer()
                val source = inputStream.source().buffer()
                sink.use {
                    source.use {
                        while (isActive) {
                            val readCount = source.read(sink.buffer, SEGMENT_SIZE)
                            if (readCount == -1L) break
                        }
                    }
                }
            }
        }.getOrNull()
    }

    private companion object {
        /**
         * Matches [okio.Segment.SIZE] which is internal to okio.
         */
        const val SEGMENT_SIZE = 8192L
    }
}
