package com.nice.cxonechat.sample.data

import android.net.Uri
import com.nice.cxonechat.message.ContentDescriptor

/**
 * DataSources that can convert a content uri into data and other associated details
 */
interface ContentDataSource {
    /**
     * Regex describing content types this data source can handle
     */
    val acceptRegex: Regex

    /**
     * test if a given mime type is acceptable to this data source
     *
     * @param mimeType mime type to test
     * @return true iff this data source can process the given mime type
     */
    fun acceptsMimeType(mimeType: String) = acceptRegex.matchEntire(mimeType) != null

    /**
     * fetch the details (primarily the content) of a content URI
     * for an attachment
     *
     * @param attachmentUri uri to process
     * @return details of uri prepared for uploading
     */
    suspend fun descriptorForUri(attachmentUri: Uri): ContentDescriptor?
}
