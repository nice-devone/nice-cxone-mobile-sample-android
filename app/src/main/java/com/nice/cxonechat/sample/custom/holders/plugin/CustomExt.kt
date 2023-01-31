package com.nice.cxonechat.sample.custom.holders.plugin

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import com.nice.cxonechat.sample.R
import com.nice.cxonechat.sample.model.PluginModel.Custom
import com.nice.cxonechat.sample.util.dpToPixels

/**
 * Class which performs mapping of [Custom] to views and binds them to the parent view.
 */
internal object CustomExt {
    internal fun Custom.bindToParent(parent: ViewGroup) = with(parent) {
        if (!fallbackText.isNullOrEmpty()) {
            val text = TextView(context)
            text.text = context.getString(R.string.message_plugin_custom_fallback_text_label, fallbackText)
            addView(text)
        }
        val color = variables["color"].toString()
        val view = View(context)
        when (color) {
            "green" -> view.setBackgroundColor(Color.GREEN)
            "blue" -> view.setBackgroundColor(Color.BLUE)
            else -> view.setBackgroundColor(Color.BLACK)
        }
        val size = context.dpToPixels(100f).toInt()
        view.layoutParams = LayoutParams(size, size)
        addView(view)
    }
}
