package com.nice.cxonechat.sample.custom.holders

import android.view.View
import android.widget.TextView
import com.nice.cxonechat.sample.R
import com.nice.cxonechat.sample.custom.holders.plugin.bindToViewGroup
import com.nice.cxonechat.sample.model.PluginMessage
import com.stfalcon.chatkit.messages.MessageHolders.OutcomingTextMessageViewHolder
import com.stfalcon.chatkit.utils.DateFormatter
import com.stfalcon.chatkit.utils.DateFormatter.Template.TIME

class OutcomingTextAndButtonsMessageViewHolder(
    itemView: View,
    payload: Any?,
) : OutcomingTextMessageViewHolder<PluginMessage>(itemView, payload) {

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

}
