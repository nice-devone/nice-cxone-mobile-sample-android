package com.nice.cxonechat.sample.custom.holders.plugin

import android.view.ViewGroup
import com.nice.cxonechat.sample.custom.holders.plugin.ButtonExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.TextExt.bindToParent
import com.nice.cxonechat.sample.model.PluginModel.QuickReplies

/**
 * Class which performs mapping of [QuickReplies] to views and binds them to the parent view.
 */
internal object QuickRepliesExt {
    fun QuickReplies.bindToParent(parent: ViewGroup) {
        this.text?.bindToParent(parent)
        buttons.forEach { it.bindToParent(parent) }
    }
}
