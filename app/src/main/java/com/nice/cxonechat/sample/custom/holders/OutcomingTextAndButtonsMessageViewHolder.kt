package com.nice.cxonechat.sample.custom.holders

import android.view.View
import android.widget.TextView
import com.nice.cxonechat.sample.R.id
import com.nice.cxonechat.sample.model.PluginMessage
import com.stfalcon.chatkit.messages.MessageHolders.OutcomingTextMessageViewHolder

class OutcomingTextAndButtonsMessageViewHolder(
    itemView: View,
    payload: Any?,
) : OutcomingTextMessageViewHolder<PluginMessage?>(itemView, payload) {

    private val tvDuration: TextView = itemView.findViewById(id.duration)
    private val tvTime: TextView = itemView.findViewById(id.time)

    override fun onBind(message: PluginMessage?) {
        super.onBind(message)
        tvDuration.text = "12min."
        tvTime.text = "custom plugin"
    }
}
