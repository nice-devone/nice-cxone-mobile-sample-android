package com.nice.cxonechat.sample.custom.holders.plugin

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.nice.cxonechat.sample.model.PluginModel.Text
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.utils.NoCopySpannableFactory

/**
 * Class which performs mapping [Text] to view and binds it to the parent view.
 */
internal object TextExt {
    internal fun Text.bindToParent(parent: ViewGroup) {
        val textView = TextView(parent.context)
        textView.setSpannableFactory(NoCopySpannableFactory.getInstance())
        if (isHtml || isMarkdown) {
            val markwon = obtainMarkwon(parent.context)
            markwon.setMarkdown(textView, text)
        } else {
            textView.text = text
        }
        parent.addView(textView)
    }


    private fun obtainMarkwon(context: Context) = Markwon.builder(context)
        .usePlugin(HtmlPlugin.create())
        .usePlugin(GlideImagesPlugin.create(context))
        .usePlugin(LinkifyPlugin.create())
        .build()
}
