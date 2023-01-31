package com.nice.cxonechat.sample.ui.main

import androidx.annotation.CheckResult
import androidx.lifecycle.ViewModel
import com.nice.cxonechat.Chat
import com.nice.cxonechat.event.ChatWindowOpenEvent
import com.nice.cxonechat.event.VisitEvent
import com.nice.cxonechat.sample.data.flow
import com.nice.cxonechat.sample.domain.ChatRepository
import com.nice.cxonechat.sample.domain.SelectedThreadRepository
import com.nice.cxonechat.sample.model.CreateThreadResult
import com.nice.cxonechat.sample.model.foldToCreateThreadResult
import com.nice.cxonechat.sample.storage.ValueStorage
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.CUSTOMER_CUSTOM_VALUES_KEY
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.FIRST_NAME_KEY
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.LAST_NAME_KEY
import com.nice.cxonechat.sample.storage.getCustomerCustomValues
import com.nice.cxonechat.sample.storage.toJson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@HiltViewModel
internal class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val valueStorage: ValueStorage,
    private val selectedThreadRepository: SelectedThreadRepository,
) : ViewModel() {

    private val chat: Chat
        get() = requireNotNull(chatRepository.chatInstance)

    private val threads by lazy { chat.threads() }

    private val events by lazy { chat.events() }

    val isMultiThreadEnabled: Boolean
        get() = chat.configuration.hasMultipleThreadsPerEndUser


    override fun onCleared() {
        chatRepository.closeConnection()
    }

    suspend fun signOut() {
        chatRepository.signOut()
    }

    @CheckResult
    fun createThread(customContactFields: Map<String, String> = emptyMap()): CreateThreadResult = runCatching {
        val handler = threads.create(customContactFields)
        selectedThreadRepository.chatThreadHandler = handler
    }.foldToCreateThreadResult()

    suspend fun isUserSetupRequired(): Boolean {
        val firstName = valueStorage.getString(FIRST_NAME_KEY).firstOrNull()
        val lastName = valueStorage.getString(LAST_NAME_KEY).firstOrNull()
        return firstName.isNullOrBlank() || lastName.isNullOrBlank()
    }

    suspend fun isFirstThread(): Boolean {
        val threadList = threads.flow.first()
        val isFirst = threadList.isEmpty()
        if (!isFirst) selectedThreadRepository.chatThreadHandler = threads.thread(threadList.first())
        return isFirst
    }

    suspend fun setUserDetails(userDetails: UserDetails) {
        chat.customFields().add(userDetails.chatCustomFields)
        valueStorage.setStrings(
            mapOf(
                FIRST_NAME_KEY to userDetails.firstName,
                LAST_NAME_KEY to userDetails.lastName,
                CUSTOMER_CUSTOM_VALUES_KEY to userDetails.chatCustomFields.toJson()
            )
        )
        selectedThreadRepository.chatThreadHandler?.customFields()?.add(userDetails.threadCustomFields)
    }

    fun setThreadName(threadName: String) {
        selectedThreadRepository.chatThreadHandler?.setName(threadName)
    }

    suspend fun getUserDetails(): UserDetails {
        val customFields: Map<String, String> = valueStorage.getCustomerCustomValues()
        return UserDetails(
            firstName = valueStorage.getString(FIRST_NAME_KEY).first(),
            lastName = valueStorage.getString(LAST_NAME_KEY).first(),
            customFields = customFields
        )
    }

    fun reportStoreVisitor() {
        events.trigger(VisitEvent)
    }

    fun reportChatWindowOpen() {
        events.trigger(ChatWindowOpenEvent)
    }

}
