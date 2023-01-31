package com.nice.cxonechat.sample.custom.holders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.nice.cxonechat.sample.R
import com.nice.cxonechat.sample.custom.holders.plugin.bindToViewGroup
import com.nice.cxonechat.sample.model.PluginMessage
import com.ouattararomuald.slider.ImageLoader.EventListener
import com.stfalcon.chatkit.messages.MessageHolders.IncomingTextMessageViewHolder
import com.stfalcon.chatkit.utils.DateFormatter
import com.stfalcon.chatkit.utils.DateFormatter.Template.TIME

class IncomingTextAndButtonsMessageViewHolder(
    itemView: View,
    payload: Any?,
) : IncomingTextMessageViewHolder<PluginMessage>(itemView, payload), EventListener {
    private val tvTime: TextView = itemView.findViewById(R.id.time)

    init {
        bubble = itemView.findViewById(R.id.bubble)
    }

    override fun onBind(message: PluginMessage) {
        super.onBind(message)
        bubble.removeAllViews()
        tvTime.text = DateFormatter.format(message.createdAt, TIME)
        val element = message.content.element
        element.bindToViewGroup(
            parent = bubble,
        ) {
            bindFallback(message)
        }
    }

    private fun bindFallback(message: PluginMessage) = with(bubble) {
        val text = TextView(context)
        text.text = message.content.postback
        addView(text)
    }

    override fun onImageViewConfiguration(imageView: ImageView) = Unit
}
