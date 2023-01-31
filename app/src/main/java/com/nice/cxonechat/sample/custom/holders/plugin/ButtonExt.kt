package com.nice.cxonechat.sample.custom.holders.plugin

import android.view.ViewGroup
import com.nice.cxonechat.sample.model.PluginModel.Button

/**
 * Class which performs mapping of [Button] to views and binds them to the parent view.
 */
internal object ButtonExt {
    internal fun Button.bindToParent(parent: ViewGroup) {
        val button = android.widget.Button(parent.context)
        button.text = text
        button.setOnClickListener(onClickAction)
        parent.addView(button)
    }
}
