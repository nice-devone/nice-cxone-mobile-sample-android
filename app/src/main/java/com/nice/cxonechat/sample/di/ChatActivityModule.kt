package com.nice.cxonechat.sample.di

import com.nice.cxonechat.Chat
import com.nice.cxonechat.sample.domain.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

@Module
@InstallIn(ActivityRetainedComponent::class)
internal class ChatActivityModule {

    @Provides
    fun produceChat(chatRepository: ChatRepository): Chat {
        return synchronized(chatRepository) {
            chatRepository.chatInstance!!
        } // TODO - find a better solution
    }

}
