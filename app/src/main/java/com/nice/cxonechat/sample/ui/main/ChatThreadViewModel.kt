package com.nice.cxonechat.sample.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatActionHandler.OnPopupActionListener
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.event.ProactiveActionClickEvent
import com.nice.cxonechat.event.ProactiveActionDisplayEvent
import com.nice.cxonechat.event.ProactiveActionFailureEvent
import com.nice.cxonechat.event.ProactiveActionSuccessEvent
import com.nice.cxonechat.event.thread.MarkThreadReadEvent
import com.nice.cxonechat.event.thread.TypingStartEvent
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.Message.Plugin
import com.nice.cxonechat.message.MessageDirection.FromApp
import com.nice.cxonechat.message.MessageDirection.ToApp
import com.nice.cxonechat.sample.data.ImageContentDataSource
import com.nice.cxonechat.sample.data.flow
import com.nice.cxonechat.sample.domain.SelectedThreadRepository
import com.nice.cxonechat.sample.model.AttachmentMessage
import com.nice.cxonechat.sample.model.Message
import com.nice.cxonechat.sample.model.PluginMessage
import com.nice.cxonechat.sample.model.PluginMessage.Content
import com.nice.cxonechat.sample.model.TextMessage
import com.nice.cxonechat.sample.model.User
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.ChatMetadata.Companion.asMetadata
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.OnPopupActionState.Empty
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.OnPopupActionState.ReceivedOnPopupAction
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.ReportOnPopupAction.CLICKED
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.ReportOnPopupAction.DISPLAYED
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.ReportOnPopupAction.FAILURE
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.ReportOnPopupAction.SUCCESS
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject
import com.nice.cxonechat.message.Message.Text as SdkTextMessage

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class ChatThreadViewModel @Inject constructor(
    private val imageContentDataSource: ImageContentDataSource,
    private val selectedThreadRepository: SelectedThreadRepository,
    private val chat: Chat,
) : ViewModel() {

    private val isMultiThreadEnabled = chat.configuration.hasMultipleThreadsPerEndUser
    private val chatThreadHandler by lazy { selectedThreadRepository.chatThreadHandler!! }
    private val chatThreadFlow = chatThreadHandler.flow

    /** Tracks messages before they are confirmed as received by backend */
    private val sentMessagesFlow: MutableStateFlow<Map<MessageId, Message>> = MutableStateFlow(emptyMap())

    private val receivedMessagesFlow: StateFlow<Map<MessageId, Message>> = chatThreadFlow
        .mapLatest(::chatThreadToMessages)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    val messages: StateFlow<List<Message>> = sentMessagesFlow
        .combine(receivedMessagesFlow) { sentMessageMap, chatUpdatePlusMessageMap -> sentMessageMap.plus(chatUpdatePlusMessageMap).values }
        .map { collection ->
            collection
                .toList()
                .sortedByDescending { message -> message.createdAt }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val agentState: StateFlow<Boolean> = chatThreadFlow
        .map { it.threadAgent?.isTyping == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val chatMetadataMutableState = chatThreadFlow
        .mapLatest { chatThread -> chatThread.asMetadata(isMultiThreadEnabled) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val chatMetadata = chatMetadataMutableState.filterNotNull()

    private val mutableActionState: MutableStateFlow<OnPopupActionState> = MutableStateFlow(Empty)
    private val actionHandler = chat.actions()
    private val messageHandler: ChatThreadMessageHandler = chatThreadHandler.messages()
    private val eventHandler = chatThreadHandler.events()

    val actionState: StateFlow<OnPopupActionState> = mutableActionState.asStateFlow()

    init {
        val listener = OnPopupActionListener { variables, metadata ->
            mutableActionState.value = ReceivedOnPopupAction(variables, metadata)
        }
        actionHandler.onPopup(listener)
    }

    fun sendMessage(message: String) {
        val appMessage: (UUID) -> Message = { id ->
            TextMessage(
                id = id.toString(),
                user = User(ChatThreadFragment.SENDER_ID, "Oscar"),
                text = message,
            )
        }
        val listener = OnMessageSentListener(appMessage, sentMessagesFlow)
        messageHandler.send(message, listener)
    }

    class OnMessageSentListener(
        private val message: (UUID) -> Message,
        flow: MutableStateFlow<Map<String, Message>>,
    ) : OnMessageTransferListener {

        private val weakRef = WeakReference(flow)
        override fun onProcessed(id: UUID) {
            val map = weakRef.get() ?: return
            val appMessage = message(id)
            map.value = map.value.plus(appMessage.id to appMessage)
        }
    }

    fun sendAttachment(message: String?, attachment: Uri) {
        viewModelScope.launch {
            val content = imageContentDataSource.uriToImageContent(attachment) ?: return@launch
            val fileName = "${UUID.randomUUID()}.jpg"
            val mimeType = "image/jpg"
            val sdkAttachment = ContentDescriptor(
                content = content,
                mimeType = mimeType,
                fileName = fileName
            )
            val appMessage: (UUID) -> Message = { id ->
                AttachmentMessage(
                    id = id.toString(),
                    user = User(ChatThreadFragment.SENDER_ID, "Oscar"),
                    text = message.orEmpty(),
                    attachment = object : Attachment() {
                        override val friendlyName: String = fileName
                        override val mimeType: String = mimeType
                        override val url: String = attachment.toString()
                    }
                )

            }
            val listener = OnMessageSentListener(appMessage, sentMessagesFlow)
            messageHandler.send(
                attachments = listOf(sdkAttachment),
                message = message.orEmpty(),
                listener = listener,
            )
        }
    }

    fun loadMore() {
        messageHandler.loadMore()
    }

    fun reportThreadRead() {
        eventHandler.trigger(MarkThreadReadEvent)
    }

    fun reportTypingStarted() {
        eventHandler.trigger(TypingStartEvent)
    }

    fun reportTypingEnd() {
        eventHandler.trigger(TypingStartEvent)
    }

    fun reportOnPopupActionDisplayed(action: ReceivedOnPopupAction) {
        chat.events().trigger(ProactiveActionDisplayEvent(action.metadata))
    }

    fun reportOnPopupActionClicked(action: ReceivedOnPopupAction) {
        chat.events().trigger(ProactiveActionClickEvent(action.metadata))
    }

    fun reportOnPopupAction(
        reportType: ReportOnPopupAction,
        action: ReceivedOnPopupAction,
    ) {
        val events = chat.events()
        when (reportType) {
            DISPLAYED -> events.trigger(ProactiveActionDisplayEvent(action.metadata))
            CLICKED -> events.trigger(ProactiveActionClickEvent(action.metadata))
            SUCCESS -> {
                events.trigger(ProactiveActionSuccessEvent(action.metadata))
                clearOnPopupAction(action)
            }
            FAILURE -> {
                events.trigger(ProactiveActionFailureEvent(action.metadata))
                clearOnPopupAction(action)
            }
        }
    }

    fun reportPageView() {
        chat.events().trigger(PageViewEvent("ChatView", "ui/main/chat-view"))
    }

    private fun chatThreadToMessages(chatThread: ChatThread): Map<MessageId, Message> {
        return chatThread.messages.associate { sdkMessage ->
            val user = User(
                id = when (sdkMessage.direction) {
                    FromApp -> ChatThreadFragment.SENDER_ID
                    ToApp -> "0"
                },
                name = sdkMessage.author.name,
            )
            val status = when (sdkMessage.direction) {
                ToApp -> "Read"
                FromApp -> if (sdkMessage.metadata.readAt != null) "Read" else "Received"
            }
            val uuid = sdkMessage.id.toString()
            val message = when (sdkMessage) {
                is SdkTextMessage -> {
                    val list = sdkMessage.attachments.toList()
                    if (list.isEmpty()) {
                        TextMessage(
                            uuid,
                            user,
                            sdkMessage.text,
                            sdkMessage.createdAt,
                        )
                    } else {
                        AttachmentMessage(
                            uuid,
                            user,
                            sdkMessage.text,
                            sdkMessage.createdAt,
                            status,
                            list.first()
                        )
                    }
                }
                is Plugin -> PluginMessage(
                    uuid,
                    user,
                    content = Content(sdkMessage)
                )
            }
            message.id to message
        }
    }

    private fun clearOnPopupAction(action: ReceivedOnPopupAction) {
        if (mutableActionState.value == action) {
            mutableActionState.value = Empty
        }
    }

    override fun onCleared() {
        actionHandler.close()
        super.onCleared()
    }

    sealed interface OnPopupActionState {
        object Empty : OnPopupActionState
        data class ReceivedOnPopupAction(val variables: Any, val metadata: ActionMetadata) : OnPopupActionState
    }

    enum class ReportOnPopupAction {
        DISPLAYED,
        CLICKED,
        SUCCESS,
        FAILURE;
    }

    data class ChatMetadata(
        val threadName: String,
        val agent: Agent?,
    ) {
        companion object {
            fun ChatThread.asMetadata(isMultiThreadEnabled: Boolean) = ChatMetadata(
                threadName = threadName ?: threadAgent?.fullName.takeIf { isMultiThreadEnabled }.orEmpty(),
                agent = threadAgent,
            )
        }
    }

}

internal typealias MessageId = String
