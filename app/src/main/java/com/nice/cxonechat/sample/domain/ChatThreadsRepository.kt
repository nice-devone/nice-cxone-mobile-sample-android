package com.nice.cxonechat.sample.domain

import android.util.Log
import com.nice.cxonechat.ChatThreadsHandler
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

@ActivityRetainedScoped
internal class ChatThreadsRepository @Inject constructor(
    chatRepository: ChatRepository,
) {

    val chat by lazy { chatRepository.chatInstance!! }
    val events by lazy { chat.events() }
    val threadsHandler: ChatThreadsHandler by lazy { chat.threads() }

    val threadUpdates by lazy {
        callbackFlow {
            val cancellable = threadsHandler.threads { threadList ->
                Log.wtf("ChatThreadsHandler", "callbackFlow $threadsHandler $this \n UPDATE: ${threadList.joinToString(separator = ",\n")}")
                trySend(threadList)
            }
            threadsHandler.refresh()
            awaitClose {
                Log.wtf("ChatThreadsHandler", "callbackFlow $threadsHandler $this \n CLOSE")
                cancellable.cancel()
            }
        }
    }
}
