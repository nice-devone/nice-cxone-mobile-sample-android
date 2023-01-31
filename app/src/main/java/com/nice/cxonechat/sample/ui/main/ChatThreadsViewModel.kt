package com.nice.cxonechat.sample.ui.main

import androidx.annotation.CheckResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.event.thread.ArchiveThreadEvent
import com.nice.cxonechat.sample.data.flow
import com.nice.cxonechat.sample.domain.SelectedThreadRepository
import com.nice.cxonechat.sample.model.CreateThreadResult
import com.nice.cxonechat.sample.model.Thread
import com.nice.cxonechat.sample.model.foldToCreateThreadResult
import com.nice.cxonechat.sample.storage.ValueStorage
import com.nice.cxonechat.sample.storage.getCustomerCustomValues
import com.nice.cxonechat.thread.ChatThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ChatThreadsViewModel @Inject constructor(
    private val chat: Chat,
    private val selectedThreadRepository: SelectedThreadRepository,
    private val valueStorage: ValueStorage,
) : ViewModel() {

    private val events = chat.events()
    private val threadsHandler = chat.threads()

    val isMultiThreadEnabled: Boolean = chat.configuration.hasMultipleThreadsPerEndUser

    val threads: StateFlow<List<Thread>> = threadsHandler
        .flow
        .conflate()
        .map { chatThreads ->
            chatThreads.map { chatThread ->
                Thread(
                    chatThread = chatThread,
                    name = if (isMultiThreadEnabled) {
                        chatThread.threadName.takeIf { !it.isNullOrBlank() } ?: "N/A"
                    } else {
                        chatThread.threadAgent?.fullName.orEmpty()
                    }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun archiveThread(thread: ChatThread) {
        threadsHandler.thread(thread).events().trigger(ArchiveThreadEvent)
    }

    fun selectThread(thread: ChatThread) {
        selectedThreadRepository.chatThreadHandler = threadsHandler.thread(thread)
    }

    @CheckResult
    suspend fun createThread(customContactFields: MutableMap<String, String>): CreateThreadResult {
        chat.customFields().add(valueStorage.getCustomerCustomValues())
        return runCatching {
            val handler: ChatThreadHandler = threadsHandler.create(customContactFields)
            selectedThreadRepository.chatThreadHandler = handler
        }.foldToCreateThreadResult()
    }

    fun reportPageView() {
        events.trigger(PageViewEvent("ChatThreadsView", "threads-view"))
    }

    fun refreshThreads() {
        viewModelScope.launch {
            threadsHandler.refresh()
        }
    }
}
