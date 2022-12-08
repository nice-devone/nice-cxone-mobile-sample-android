package com.nice.cxonechat.sample.model

import com.nice.cxonechat.message.Message.Plugin
import com.nice.cxonechat.message.Message.Text
import com.nice.cxonechat.thread.ChatThread
import java.util.*

data class Thread(
    val chatThread: ChatThread,
    val name: String,
) {

    val id: UUID = chatThread.id

    /**
     * Last message converted to text (if possible).
     */
    val lastMessage: String = chatThread.messages
        .asSequence()
        .sortedByDescending { it.createdAt }
        .firstOrNull()
        ?.run {
            when (this) {
                is Text -> text
                is Plugin -> "Plugin message - content unavailable" // TODO implement
            }
        }
        .orEmpty()

    val agentImage: String = chatThread.threadAgent?.imageUrl.orEmpty()
}
