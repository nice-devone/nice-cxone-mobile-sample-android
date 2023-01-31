package com.nice.cxonechat.sample.custom.holders.plugin

import android.util.Log
import android.view.ViewGroup
import com.nice.cxonechat.sample.custom.holders.plugin.ButtonExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.CustomExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.FileExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.GalleryExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.MenuExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.QuickRepliesExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.SatisfactionSurveyExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.SubtitleExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.TextAndButtonsExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.TextExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.TitleExt.bindToParent
import com.nice.cxonechat.sample.model.PluginModel
import com.nice.cxonechat.sample.model.PluginModel.Button
import com.nice.cxonechat.sample.model.PluginModel.Custom
import com.nice.cxonechat.sample.model.PluginModel.File
import com.nice.cxonechat.sample.model.PluginModel.Gallery
import com.nice.cxonechat.sample.model.PluginModel.Menu
import com.nice.cxonechat.sample.model.PluginModel.QuickReplies
import com.nice.cxonechat.sample.model.PluginModel.SatisfactionSurvey
import com.nice.cxonechat.sample.model.PluginModel.Subtitle
import com.nice.cxonechat.sample.model.PluginModel.Text
import com.nice.cxonechat.sample.model.PluginModel.TextAndButtons
import com.nice.cxonechat.sample.model.PluginModel.Title

/**
 * Converts [PluginModel] (if it is a supported type) to views and binds them to the supplied [parent] [ViewGroup].
 * If the supplied element is not supported (or null), then [fallbackBinding] is used instead.
 */
internal fun PluginModel?.bindToViewGroup(
    parent: ViewGroup,
    fallbackBinding: ViewGroup.() -> Unit,
) {
    when (this) {
        is File -> bindToParent(parent)
        is Title -> bindToParent(parent)
        is Text -> bindToParent(parent)
        is Button -> bindToParent(parent)
        is Custom -> bindToParent(parent)
        is Menu -> bindToParent(parent)
        is Subtitle -> bindToParent(parent)
        is TextAndButtons -> bindToParent(parent)
        is Gallery -> bindToParent(parent, fallbackBinding)
        is SatisfactionSurvey -> bindToParent(parent)
        is QuickReplies -> bindToParent(parent)
        else -> {
            Log.w("PluginMessageViewHolder", "Discarding plugin message: $this")
            parent.fallbackBinding()
        }
    }
}
