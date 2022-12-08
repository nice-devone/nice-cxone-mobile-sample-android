package com.nice.cxonechat.sample.domain

import android.content.Context
import androidx.annotation.GuardedBy
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.sample.BuildConfig
import com.nice.cxonechat.sample.storage.ChatConfigurationStorage.getConfiguration
import com.nice.cxonechat.sample.storage.EnvironmentStorage.getEnvironment
import com.nice.cxonechat.sample.storage.ValueStorage
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.AUTH_TOKEN_KEY
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.CODE_VERIFIER_KEY
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
internal class ChatRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val valueStorage: ValueStorage,
) {

    @GuardedBy("this")
    @Volatile
    private var chat: Chat? = null
    val chatInstance: Chat?
        get() = synchronized(this) {
            chat
        }

    suspend fun createChat(skipAuthorization: Boolean = false): Chat {
        val authorization = if (skipAuthorization) null else createAuthorization()
        // TODO add support for missing config
        val environment = valueStorage.getEnvironment()
        val socketFactoryConfiguration = requireNotNull(valueStorage.getConfiguration(environment))
        val builder = with(ChatBuilder(context = context, config = socketFactoryConfiguration)) {
            authorization?.let(::setAuthorization)
            setDevelopmentMode(BuildConfig.DEBUG)
        }
        val newChat = builder.awaitBuild()
        val token = Firebase.messaging.token
        if (token.isSuccessful) {
            newChat.setDeviceToken(token.result)
        }
        synchronized(this) {
            chat?.close()
            chat = newChat
        }
        return newChat
    }

    suspend fun signOut() {
        synchronized(this) {
            chat?.signOut()
            chat = null
        }
        valueStorage.clear()
    }

    fun closeConnection() {
        synchronized(this) {
            chat?.close()
            chat = null
        }
    }

    private suspend inline fun createAuthorization(): Authorization? {
        val code = valueStorage.getString(CODE_VERIFIER_KEY).firstNotBlankOrNull() ?: return null
        val authToken = valueStorage.getString(AUTH_TOKEN_KEY).firstNotBlankOrNull() ?: return null
        return Authorization(code, authToken)
    }

    private suspend inline fun Flow<String>.firstNotBlankOrNull(): String? = firstOrNull()?.takeIf(String::isNotBlank)

    private suspend fun ChatBuilder.awaitBuild(): Chat = suspendCancellableCoroutine { continuation ->
        val cancellable: Cancellable = build(continuation::resume)
        continuation.invokeOnCancellation { cancellable.cancel() }
    }
}
