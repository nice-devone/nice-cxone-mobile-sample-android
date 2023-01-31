package com.nice.cxonechat.sample.custom.holders.plugin

import android.view.ViewGroup
import com.nice.cxonechat.sample.custom.holders.plugin.ButtonExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.TextExt.bindToParent
import com.nice.cxonechat.sample.model.PluginModel.TextAndButtons

/**
 * Class which performs mapping [TextAndButtons] to views and binds them to the parent view.
 */
internal object TextAndButtonsExt {
    internal fun TextAndButtons.bindToParent(parent: ViewGroup) {
        text.bindToParent(parent)
        for (button in buttons) {
            button.bindToParent(parent)
        }
    }
}
