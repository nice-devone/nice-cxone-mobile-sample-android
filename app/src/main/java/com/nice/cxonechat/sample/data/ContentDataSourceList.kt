package com.nice.cxonechat.sample.data

import android.content.Context
import android.net.Uri
import com.nice.cxonechat.message.ContentDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * list of available [ContentDataSource] which can be used
 * to process an content URI for attachment
 *
 * @property context for content resolving
 * @param imageContentDataSource [ContentDataSource] for images
 * @param documentContentDataSource [ContentDataSource] for videos and pdf
 * documents and other attachments that are treated as raw data
 */
@Singleton
internal class ContentDataSourceList @Inject constructor(
    @ApplicationContext private val context: Context,
    imageContentDataSource: ImageContentDataSource,
    documentContentDataSource: DocumentContentDataSource
) {
    private val dataSources = listOf(
        imageContentDataSource,
        documentContentDataSource
    )

    /**
     * search available data sources for one that can handle the requested uri.
     *
     * @param uri attachment URI to process
     * @return [ContentDescriptor] for attachment upload if an appropriate data source
     * was found.  null if no data source could be found
     */
    suspend fun descriptorForUri(uri: Uri): ContentDescriptor? {
        val mimeType = context.contentResolver.getType(uri) ?: return null

        return dataSources
                .firstOrNull { it.acceptsMimeType(mimeType) }
                ?.descriptorForUri(uri)
    }
}
