package com.nice.cxonechat.sample.custom.holders.plugin

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_XY
import com.bumptech.glide.Glide
import com.nice.cxonechat.sample.R
import com.nice.cxonechat.sample.model.PluginModel.File

/**
 * Class which performs mapping [File] to an image view and binds it to the parent view.
 */
internal object FileExt {
    internal fun File.bindToParent(parent: ViewGroup) {
        val imageView = ImageView(parent.context)
        imageView.scaleType = FIT_XY
        Glide.with(parent)
            .load(url)
            .placeholder(R.drawable.downloading_48px)
            .fallback(R.drawable.document_48px)
            .error(R.drawable.error_48px)
            .into(imageView)
        parent.addView(imageView)
    }
}
