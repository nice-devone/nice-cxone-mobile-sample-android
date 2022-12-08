package com.nice.cxonechat.sample.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ImageContentDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun uriToImageContent(imageUri: Uri): String? {
        val imageContent = runInterruptible(Dispatchers.IO) {
            val bitmap: Bitmap = MediaStore.Images.Media
                .getBitmap(context.contentResolver, imageUri)
                ?: return@runInterruptible null

            ByteArrayOutputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            }
        }
        return imageContent
    }
}
