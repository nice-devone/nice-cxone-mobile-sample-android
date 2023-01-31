package com.nice.cxonechat.sample.custom.holders.plugin

import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import com.nice.cxonechat.sample.model.PluginModel.Title

/**
 * Class which performs mapping [Title] to view and binds it to the parent view.
 */
internal object TitleExt {
    internal fun Title.bindToParent(parent: ViewGroup) {
        val textView = TextView(parent.context)
        textView.text = text
        textView.textSize = 16f
        textView.typeface = Typeface.DEFAULT_BOLD
        parent.addView(textView)
    }
}
