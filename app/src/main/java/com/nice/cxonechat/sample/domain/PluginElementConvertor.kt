package com.nice.cxonechat.sample.domain

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import com.nice.cxonechat.Chat
import com.nice.cxonechat.event.CustomVisitorEvent
import com.nice.cxonechat.message.Message.Plugin
import com.nice.cxonechat.message.PluginElement
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.model.PluginMessage.Content
import com.nice.cxonechat.sample.model.PluginModel
import javax.inject.Inject
import javax.inject.Provider

/**
 * Converts [PluginElement] to [PluginModel].
 * Main focus of this class is to resolve [PluginElement.Button] deeplinks and postbacks to onClickAction,
 * which should be performed when the resulting ui button widget is clicked.
 */
internal class PluginElementConvertor @Inject constructor(
    private val chatProvider: Provider<Chat>,
) {

    fun pluginToContent(plugin: Plugin): Content {
        val postback = plugin.postback?.takeIf { it.isNotBlank() } ?: "Unsupported custom message"
        return Content(postback, createModel(pluginElement = plugin.element))
    }

    @Suppress(
        "ComplexMethod" // Exhaustive when
    )
    private fun createModel(pluginElement: PluginElement?) = when (pluginElement) {
        is PluginElement.Menu -> createModel(pluginElement)
        is PluginElement.File -> createModel(pluginElement)
        is PluginElement.Title -> createModel(pluginElement)
        is PluginElement.Subtitle -> createModel(pluginElement)
        is PluginElement.Text -> createModel(pluginElement)
        is PluginElement.Button -> createModel(pluginElement)
        is PluginElement.TextAndButtons -> createModel(pluginElement)
        is PluginElement.QuickReplies -> createModel(pluginElement)
        is PluginElement.InactivityPopup -> PluginModel.InactivityPopup
        is PluginElement.Countdown -> PluginModel.Countdown
        is PluginElement.Custom -> createModel(pluginElement)
        is PluginElement.Gallery -> createModel(pluginElement)
        is PluginElement.SatisfactionSurvey -> createModel(pluginElement)
        null -> null
    }

    private fun createModel(element: PluginElement.TextAndButtons): PluginModel.TextAndButtons = PluginModel.TextAndButtons(
        text = createModel(element.text),
        buttons = element.buttons.map(::createModel)
    )

    private fun createModel(menu: PluginElement.Menu): PluginModel.Menu = PluginModel.Menu(
        files = menu.files.map(::createModel),
        titles = menu.titles.map(::createModel),
        subtitles = menu.subtitles.map(::createModel),
        texts = menu.texts.map(::createModel),
        buttons = menu.buttons.map(::createModel),
    )

    private fun createModel(button: PluginElement.Button): PluginModel.Button {
        val actions = buildList {
            val postback = button.postback
            if (!postback.isNullOrBlank()) {
                add(PostBackAction(chatProvider, postback))
            }
            val deepLink = button.deepLink
            if (!deepLink.isNullOrBlank()) {
                add(DeepLinkAction(deepLink))
            }
        }
        return PluginModel.Button(
            text = button.text,
            displayInApp = button.displayInApp,
            onClickAction = CompoundAction(actions),
        )
    }

    private fun createModel(file: PluginElement.File): PluginModel.File = PluginModel.File(file.url, file.name, file.mimeType)

    private fun createModel(title: PluginElement.Title): PluginModel.Title = PluginModel.Title(title.text)

    private fun createModel(subtitle: PluginElement.Subtitle): PluginModel.Subtitle = PluginModel.Subtitle(subtitle.text)

    private fun createModel(text: PluginElement.Text): PluginModel.Text = PluginModel.Text(text.text, text.isMarkdown, text.isHtml)

    private fun createModel(custom: PluginElement.Custom): PluginModel.Custom = PluginModel.Custom(
        fallbackText = custom.fallbackText,
        variables = custom.variables
    )

    private fun createModel(gallery: PluginElement.Gallery): PluginModel.Gallery = PluginModel.Gallery(
        gallery.elements.mapNotNull(::createModel)
    )

    private fun createModel(satisfactionSurvey: PluginElement.SatisfactionSurvey): PluginModel.SatisfactionSurvey = PluginModel.SatisfactionSurvey(
        text = satisfactionSurvey.text?.let(::createModel),
        button = createModel(satisfactionSurvey.button),
        postback = satisfactionSurvey.postback,
    )

    private fun createModel(quickReplies: PluginElement.QuickReplies): PluginModel.QuickReplies = PluginModel.QuickReplies(
        text = quickReplies.text?.let(::createModel),
        buttons = quickReplies.buttons.map(::createModel),
    )
}

/**
 * Class which will invoke all provided [actions] in given order.
 */
private class CompoundAction(
    private val actions: Iterable<(View) -> Unit>,
) : (View) -> Unit {
    override fun invoke(view: View) {
        for (action in actions) {
            action(view)
        }
    }
}

/**
 * Action which will trigger chat event [CustomVisitorEvent] with supplied [postback] as the event's value.
 */
private class PostBackAction(
    private val chatProvider: Provider<Chat>,
    private val postback: String,
) : (View) -> Unit {
    override fun invoke(view: View) {
        chatProvider.get().events().trigger(CustomVisitorEvent(postback))
    }
}

/**
 * Action which will start an Activity using the supplied [deepLink] as an [Uri] for the Intent.
 */
private class DeepLinkAction(
    private val deepLink: String,
) : (View) -> Unit {
    override fun invoke(view: View) {
        try {
            val uri = Uri.parse(deepLink)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            view.context.startActivity(intent)
        } catch (expected: ActivityNotFoundException) {
            AlertDialog.Builder(view.context)
                .setTitle("Information")
                .setMessage(expected.message)
                .setPositiveButton(string.ok) { dialog, _ -> dialog?.dismiss() }
                .show()
        }
    }

}
