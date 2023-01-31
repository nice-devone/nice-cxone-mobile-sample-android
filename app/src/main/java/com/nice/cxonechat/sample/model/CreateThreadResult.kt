package com.nice.cxonechat.sample.model

import com.nice.cxonechat.exceptions.MissingThreadListFetchException
import com.nice.cxonechat.exceptions.UnsupportedChannelConfigException
import com.nice.cxonechat.sample.model.CreateThreadResult.Failure
import com.nice.cxonechat.sample.model.CreateThreadResult.Failure.GENERAL_FAILURE
import com.nice.cxonechat.sample.model.CreateThreadResult.Success

/**
 * Possible results of thread creation action.
 */
internal sealed interface CreateThreadResult {
    object Success : CreateThreadResult
    enum class Failure : CreateThreadResult {
        REASON_THREADS_REFRESH_REQUIRED,
        REASON_THREAD_CREATION_FORBIDDEN,
        GENERAL_FAILURE,
    }
}

/**
 * Utility transformation method for conversion of [Result] to [CreateThreadResult], assuming that the result is
 * product of [runCatching] executed over [com.nice.cxonechat.ChatThreadsHandler.thread] method.
 */
internal fun <T> Result<T>.foldToCreateThreadResult(): CreateThreadResult = fold(
    onSuccess = { Success },
    onFailure = { throwable ->
        when (throwable) {
            is UnsupportedChannelConfigException -> Failure.REASON_THREAD_CREATION_FORBIDDEN
            is MissingThreadListFetchException -> Failure.REASON_THREADS_REFRESH_REQUIRED
            else -> GENERAL_FAILURE
        }
    }
)
