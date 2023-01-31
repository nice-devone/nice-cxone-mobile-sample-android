package com.nice.cxonechat.sample.custom.holders.plugin

import android.view.ViewGroup
import com.nice.cxonechat.sample.custom.holders.plugin.ButtonExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.TextExt.bindToParent
import com.nice.cxonechat.sample.model.PluginModel.SatisfactionSurvey

/**
 * Class which performs mapping of [SatisfactionSurvey] to views and binds them to the parent view.
 */
internal object SatisfactionSurveyExt {
    fun SatisfactionSurvey.bindToParent(parent: ViewGroup) {
        text?.bindToParent(parent)
        button.bindToParent(parent)
    }
}
