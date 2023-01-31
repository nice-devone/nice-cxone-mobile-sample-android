package com.nice.cxonechat.sample.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import com.nice.cxonechat.message.ContentDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runInterruptible
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ImageContentDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : ContentDataSource {
    override val acceptRegex = Regex("image/.*")

    override suspend fun descriptorForUri(attachmentUri: Uri): ContentDescriptor? {
        return runInterruptible {
            ContentDescriptor(
                content = getContent(attachmentUri) ?: return@runInterruptible null,
                mimeType = "image/jpg",
                fileName = "${UUID.randomUUID()}.jpg"
            )
        }
    }

    private fun getContent(imageUri: Uri) =
        context.bitmapForUri(imageUri)?.let { bitmap ->
            ByteArrayOutputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            }
        }

    private fun Context.bitmapForUri(uri: Uri): Bitmap? =
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            @Suppress("Deprecation")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        } else {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        }
}
