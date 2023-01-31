package com.nice.cxonechat.sample.util

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Schedules the request to be executed at some time in the future, same as [Call.enqueue],
 * but wraps the callback in [suspendCancellableCoroutine].
 * In case of failure, or if the response has an empty body, the coroutine is resumed with exception.
 *
 * @see [Call.enqueue].
 */
suspend fun Call.await(): ResponseBody {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancel()
        }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body ?: response.cacheResponse?.body
                    if (body == null) {
                        val method = call.request().method
                        val e = KotlinNullPointerException("Response from " +
                                method +
                                " was null but response body type was declared as non-null")
                        continuation.resumeWithException(e)
                    } else {
                        continuation.resume(body)
                    }
                } else {
                    continuation.resumeWithException(IOException(response.toString()))
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }
}
