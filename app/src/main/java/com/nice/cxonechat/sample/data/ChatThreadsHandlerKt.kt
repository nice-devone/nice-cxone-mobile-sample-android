package com.nice.cxonechat.sample.data

import com.nice.cxonechat.ChatThreadsHandler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow


internal val ChatThreadsHandler.flow
    get() = callbackFlow {
        val cancellable = threads { threadList ->
            trySend(threadList)
        }
        refresh()
        awaitClose {
            cancellable.cancel()
        }
    }
