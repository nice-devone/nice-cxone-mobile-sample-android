package com.nice.cxonechat.sample.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.nice.cxonechat.sample.R
import java.io.File

/**
 * File provider for attachments which are stored via [TemporaryFileStorage].
 */
class TemporaryFileProvider : FileProvider(R.xml.tmp_file_path) {

    internal companion object {
        private const val AUTHORITY = "com.nice.cxonechat.fileprovider"
        fun getUriForFile(file: File, filename: String, context: Context): Uri =
            getUriForFile(context, AUTHORITY, file, filename)
    }
}
