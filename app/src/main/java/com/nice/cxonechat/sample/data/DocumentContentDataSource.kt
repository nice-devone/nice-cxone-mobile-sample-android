package com.nice.cxonechat.sample.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import com.nice.cxonechat.message.ContentDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runInterruptible
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ContentDataSource] for videos, pdf documents and other attachments that are
 * treated as raw data
 *
 * @property context Context for content resolver
 */
@Singleton
internal class DocumentContentDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : ContentDataSource {
    override val acceptRegex = Regex("""(video/.*|application/pdf)""")

    override suspend fun descriptorForUri(attachmentUri: Uri): ContentDescriptor? {
        return runInterruptible {
            val suffix = MimeTypeMap.getFileExtensionFromUrl(attachmentUri.toString()) ?: return@runInterruptible null

            ContentDescriptor(
                content = getContent(attachmentUri) ?: return@runInterruptible null,
                mimeType = context.contentResolver.getType(attachmentUri) ?: return@runInterruptible null,
                fileName = "${UUID.randomUUID()}.$suffix"
            )
        }
    }

    private fun getContent(uri: Uri): String? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bytes = ByteArray(inputStream.available())
            inputStream.read(bytes)
            Base64.encodeToString(bytes, Base64.DEFAULT)
        }
    }
}
