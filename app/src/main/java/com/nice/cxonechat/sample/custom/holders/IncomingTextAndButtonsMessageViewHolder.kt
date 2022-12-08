package com.nice.cxonechat.sample.custom.holders

import android.app.AlertDialog.Builder
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_XY
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.message.PluginElement
import com.nice.cxonechat.message.PluginElement.Custom
import com.nice.cxonechat.message.PluginElement.File
import com.nice.cxonechat.message.PluginElement.Menu
import com.nice.cxonechat.message.PluginElement.Text
import com.nice.cxonechat.message.PluginElement.Title
import com.nice.cxonechat.sample.R.id
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.custom.holders.IncomingTextAndButtonsMessageViewHolder.Companion.PostBackPolyContent.DeepLink
import com.nice.cxonechat.sample.custom.holders.IncomingTextAndButtonsMessageViewHolder.Companion.PostBackPolyContent.Unknown
import com.nice.cxonechat.sample.model.PluginMessage
import com.nice.cxonechat.util.RuntimeTypeAdapterFactory
import com.ouattararomuald.slider.ImageLoader
import com.ouattararomuald.slider.ImageLoader.EventListener
import com.ouattararomuald.slider.ImageLoader.Factory
import com.ouattararomuald.slider.ImageSlider
import com.ouattararomuald.slider.SliderAdapter
import com.stfalcon.chatkit.messages.MessageHolders.IncomingTextMessageViewHolder
import com.stfalcon.chatkit.utils.DateFormatter
import com.stfalcon.chatkit.utils.DateFormatter.Template.TIME
import org.json.JSONException
import com.nice.cxonechat.message.PluginElement.Button as PluginButton

class IncomingTextAndButtonsMessageViewHolder(
    itemView: View,
    payload: Any?,
) : IncomingTextMessageViewHolder<PluginMessage>(itemView, payload), EventListener {
    private val bubble: LinearLayout = itemView.findViewById(id.bubble)
    private val tvTime: TextView = itemView.findViewById(id.time)
    private val imageSlider: ImageSlider = itemView.findViewById(id.image_slider)

    override fun onBind(message: PluginMessage) {
        super.onBind(message)
        tvTime.text = DateFormatter.format(message.createdAt, TIME)
        val elements = message.content.elements
        if (elements.isEmpty()) {
            bubble.removeAllViews()
            val text = TextView(bubble.context)
            text.text = message.content.postback
        } else {
            // Right now if elements contain menu, it takes precedence. TODO investigate if this intentional
            elements
                .asSequence()
                .filterIsInstance<Menu>()
                .map(::toView)
                .firstOrNull()
                ?.let {
                    bubble.addView(it)
                    return
                }

            elements.asSequence()
                .mapNotNull(::toView)
                .forEach(bubble::addView)
        }

    }


    override fun onImageViewConfiguration(imageView: ImageView) = Unit

    private fun toView(pluginElement: PluginElement): View? {
        return when (pluginElement) {
            is File -> pluginElement.toView()
            is Title -> pluginElement.toView()
            is Text -> pluginElement.toView()
            is PluginButton -> pluginElement.toView()
            is Custom -> pluginElement.toView()
            else -> {
                Log.w("PluginMessageViewHolder", "Discarding plugin element: $pluginElement")
                null // Unsupported
            }
        }
    }

    private fun toView(menu: Menu): View {
        val images = menu.files.associate {
            it.name to it.url
        }
        imageSlider.adapter = SliderAdapter(
            bubble.context,
            GlideImageLoaderFactory,
            images.values.toList(),
            images.keys.toList(),
            "slider"
        )
        return imageSlider
    }

    private fun File.toView(): View {
        val imageView = ImageView(bubble.context)
        imageView.scaleType = FIT_XY
        Glide.with(bubble)
            .load(url)
            .into(imageView)
        return imageView
    }

    private fun Title.toView(): View {
        val textView = TextView(bubble.context)
        textView.text = text
        textView.textSize = 16f
        textView.typeface = Typeface.DEFAULT_BOLD
        return textView
    }

    private fun Text.toView(): View {
        val textView = TextView(bubble.context)
        textView.text = text
        return textView
    }

    private fun PluginButton.toView(): View {
        val button = Button(bubble.context)
        button.text = this.text
        if (postback.isNotEmpty()) {
            button.setupButtonDeeplink(postback)
        }
        return button
    }

    private fun Custom.toView(): View {
        val color = variables["color"].toString()
        val view = View(bubble.context)
        when (color) {
            "green" -> view.setBackgroundColor(Color.GREEN)
            "blue" -> view.setBackgroundColor(Color.BLUE)
            else -> view.setBackgroundColor(Color.BLACK)
        }
        view.layoutParams = LayoutParams(100, 100)
        return view
    }

    private fun Button.setupButtonDeeplink(postback: String) {
        try {
            val deepLink: PostBackPolyContent? = runCatching {
                gson.fromJson(postback, PostBackPolyContent::class.java)
            }.getOrNull()
            if (deepLink !is DeepLink) return
            val deepLinkUrl = deepLink.deepLink
            setOnClickListener {
                try {
                    val uri = Uri.parse(deepLinkUrl)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                } catch (expected: Exception) {
                    Builder(context)
                        .setTitle("Information")
                        .setMessage(expected.message)
                        .setPositiveButton(string.ok) { dialog, _ -> dialog?.dismiss() }
                        .show()
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private companion object {

        private val postbackContentAdapter by lazy {
            RuntimeTypeAdapterFactory.of(PostBackPolyContent::class.java, "type")
                .registerSubtype(DeepLink::class.java, "deeplink")
                .registerDefault(Unknown)
        }

        private val gson by lazy {
            GsonBuilder()
                .registerTypeAdapterFactory(postbackContentAdapter)
                .create()
        }

        private sealed interface PostBackPolyContent {
            data class DeepLink(
                @SerializedName("deeplink")
                val deepLink: String,
            ) : PostBackPolyContent

            object Unknown : PostBackPolyContent
        }

        private object GlideImageLoaderFactory : Factory<ImageLoader> {
            override fun create(): ImageLoader = GlideImageLoader
        }

        private object GlideImageLoader : ImageLoader() {
            override fun load(path: String, imageView: ImageView) {
                Glide.with(imageView)
                    .load(path)
                    .into(imageView)
            }
        }

    }

}
