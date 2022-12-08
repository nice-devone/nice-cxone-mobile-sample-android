package com.nice.cxonechat.sample.model

import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.Message.Plugin
import com.nice.cxonechat.message.PluginElement
import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.commons.models.IUser
import com.stfalcon.chatkit.commons.models.MessageContentType
import com.stfalcon.chatkit.commons.models.MessageContentType.Image
import java.util.*

sealed class Message constructor(
    private val id: String,
    private val user: User,
    private val text: String,
    private val createdAt: Date = Date(),
    val status: String = "Sent",
) : IMessage {

    // Kotlin compiler reports issue with accidental overload of the methods, but doesn't allow intentional one

    override fun getId(): String = id
    override fun getUser(): IUser = user
    override fun getText(): String = text
    override fun getCreatedAt(): Date = createdAt

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (id != other.id) return false
        if (user != other.user) return false
        if (text != other.text) return false
        if (createdAt != other.createdAt) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }


}

class TextMessage(
    id: String,
    user: User,
    text: String,
    createdAt: Date = Date(),
    status: String = "Sent",
) : Message(id, user, text, createdAt, status)

class AttachmentMessage(
    id: String,
    user: User,
    text: String,
    createdAt: Date = Date(),
    status: String = "Sent",
    private val attachment: Attachment,
) : Message(id, user, text.ifBlank { null } ?: attachment.friendlyName, createdAt, status), Image {

    val mimeType = attachment.mimeType

    override fun getImageUrl(): String = attachment.url

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as AttachmentMessage

        if (attachment != other.attachment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + attachment.hashCode()
        return result
    }
}

class PluginMessage(
    id: String,
    user: User,
    text: String = "",
    createdAt: Date = Date(),
    status: String = "Sent",
    val content: Content,
) : Message(id, user, text, createdAt, status), MessageContentType {
    data class Content(
        val postback: String?,
        val elements: List<PluginElement>,
    ) {
        constructor(sdkMessage: Plugin) : this(sdkMessage.postback, sdkMessage.elements.toList())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PluginMessage

        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }
}
