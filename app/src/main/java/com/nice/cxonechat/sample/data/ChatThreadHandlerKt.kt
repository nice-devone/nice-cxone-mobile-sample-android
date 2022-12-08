package com.nice.cxonechat.sample.data

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal val ChatThreadHandler.flow: Flow<ChatThread>
    get() = callbackFlow {
        val cancellable = get(::trySend)
        send(get())
        refresh()
        awaitClose(cancellable::cancel)
    }
