package com.nice.cxonechat.sample.model

import android.view.View
import com.nice.cxonechat.sample.model.PluginModel.Gallery

/**
 * Internal version of [com.nice.cxonechat.message.PluginElement],
 * which replaces implementation of [com.nice.cxonechat.message.PluginElement.Button]
 * with a version, which has already converted deeplink/postback to onClickAction.
 * This is recursively for all types which can contain the button or other container types (eg. [Gallery]]).
 *
 * @see com.nice.cxonechat.message.PluginElement
 */
@Suppress(
    "UndocumentedPublicProperty" // Properties are copied
)
sealed interface PluginModel {

    /**
     * @see com.nice.cxonechat.message.PluginElement.Menu
     */
    data class Menu(
        val files: Iterable<File>,
        val titles: Iterable<Title>,
        val subtitles: Iterable<Subtitle>,
        val texts: Iterable<Text>,
        val buttons: Iterable<Button>,
    ) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.File
     */
    data class File(
        val url: String,
        val name: String,
        val mimeType: String,
    ) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.Title
     */
    data class Title(val text: String) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.Subtitle
     */
    data class Subtitle(val text: String) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.Text
     */
    data class Text(
        @Suppress(
            "MemberNameEqualsClassName"
        )
        val text: String,
        val isMarkdown: Boolean,
        val isHtml: Boolean,
    ) : PluginModel

    /**
     * Modified version of [com.nice.cxonechat.message.PluginElement.Button]
     * with [com.nice.cxonechat.message.PluginElement.Button.deepLink] and
     * [com.nice.cxonechat.message.PluginElement.Button.postback] already converted to [onClickAction].
     *
     * @property text Same as [com.nice.cxonechat.message.PluginElement.Button.text].
     * @property displayInApp Same as [com.nice.cxonechat.message.PluginElement.Button.displayInApp].
     * @property onClickAction Function which should be called as part of [View.OnClickListener.onClick].
     *
     * @see [com.nice.cxonechat.message.PluginElement.Button]
     */
    data class Button(
        val text: String,
        val displayInApp: Boolean,
        val onClickAction: (View) -> Unit,
    ) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.TextAndButtons
     */
    data class TextAndButtons(
        val text: Text,
        val buttons: Iterable<Button>,
    ) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.QuickReplies
     */
    data class QuickReplies(
        val text: Text?,
        val buttons: Iterable<Button>,
    ) : PluginModel

    /**
     * Currently unsupported.
     */
    object InactivityPopup : PluginModel

    /**
     * Currently unsupported.
     */
    object Countdown : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.Custom
     */
    data class Custom(
        val fallbackText: String?,
        val variables: Map<String, Any?>,
    ) : PluginModel

    /**
     * Modified version of [com.nice.cxonechat.message.PluginElement.Gallery]
     * with [elements] type changed to [PluginModel].
     *
     * @see com.nice.cxonechat.message.PluginElement.Gallery
     */
    data class Gallery(val elements: Iterable<PluginModel>) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.SatisfactionSurvey
     */
    data class SatisfactionSurvey(
        val text: Text?,
        val button: Button,
        val postback: String?,
    ) : PluginModel

}
