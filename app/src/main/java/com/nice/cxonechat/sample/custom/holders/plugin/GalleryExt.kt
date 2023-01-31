package com.nice.cxonechat.sample.custom.holders.plugin

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nice.cxonechat.sample.R
import com.nice.cxonechat.sample.model.PluginModel.Gallery

/**
 * Class which performs mapping [Gallery] to views and binds them to the parent view.
 */
internal object GalleryExt {
    fun Gallery.bindToParent(parent: ViewGroup, fallbackBinding: ViewGroup.() -> Unit) {
        elements.forEach { element ->
            val wrap = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery_element, parent, false) as ViewGroup
            element.bindToViewGroup(wrap) {
                wrap.fallbackBinding()
            }
            parent.addView(wrap)
        }
    }
}
