package com.nice.cxonechat.sample.custom.holders.plugin

import android.view.ViewGroup
import android.widget.TextView
import com.nice.cxonechat.sample.model.PluginModel.Subtitle

/**
 * Class which performs mapping of [Subtitle] to view and binds it to the parent view.
 */
internal object SubtitleExt {
    internal fun Subtitle.bindToParent(parent: ViewGroup) {
        val textView = TextView(parent.context)
        textView.text = text
        textView.textSize = 16f
        parent.addView(parent)
    }
}
