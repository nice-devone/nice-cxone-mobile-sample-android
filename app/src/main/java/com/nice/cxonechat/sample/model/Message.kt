package com.nice.cxonechat.sample.model

import com.nice.cxonechat.message.Attachment
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
) : Message(id, user, text.ifBlank { attachment.friendlyName }, createdAt, status), Image {
    val mimeType = attachment.mimeType

    /**
     * The original attachment url, this should be used to access the actual
     * attachment instead of [getImageUrl()] since that may be mangled to
     * support placeholders.
     */
    val originalUrl = attachment.url

    /**
     * get the image url for the in message thumbnail.
     *
     * The original url for the real resource is separately cached in [originalUrl]
     *
     * @return returns the original url for images or videos or an android.resource
     * url for the document place holder for any other mime type.
      */
    override fun getImageUrl(): String? = when {
        /* a null url will result in an error preview displayed */
        mimeType == null -> null
        mimeType.startsWith("image/") -> attachment.url
        mimeType.startsWith("video/") -> attachment.url
        /* this will result in a document preview being displayed */
        else -> "android.resource://com.nice.cxonechat.sample/drawable/document_48px"
    }

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
    /**
     * Content of the plugin message as delivered by the SDK.
     *
     * @property postback The raw postback value, can be used as a fallback text or can be parsed to an object.
     * @property element Optional [PluginModel], in case it is null the element type was unsupported by the SDK.
     */
    data class Content(
        val postback: String?,
        val element: PluginModel?,
    )

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
