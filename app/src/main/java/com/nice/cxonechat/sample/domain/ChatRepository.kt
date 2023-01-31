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
import com.nice.cxonechat.sample.storage.getAuthKey
import com.nice.cxonechat.sample.storage.getCoveVerifier
import com.nice.cxonechat.sample.storage.getCustomerCustomValues
import com.nice.cxonechat.sample.storage.getUserFirstName
import com.nice.cxonechat.sample.storage.getUserLastName
import dagger.hilt.android.qualifiers.ApplicationContext
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
        val builder: ChatBuilder = with(ChatBuilder(context = context, config = socketFactoryConfiguration)) {
            val firstName = valueStorage.getUserFirstName()
            val lastName = valueStorage.getUserLastName()
            if (firstName != null && lastName != null) {
                setUserName(
                    first = firstName,
                    last = lastName,
                )
            }
            authorization?.let(::setAuthorization)
            setDevelopmentMode(BuildConfig.DEBUG)
        }
        val newChat = builder.awaitBuild()
        val token = Firebase.messaging.token
        if (token.isSuccessful) {
            newChat.setDeviceToken(token.result)
        } else {
            newChat.setDeviceToken("") // Temporary workaround for bug affecting StoreVisitor events
        }
        newChat.customFields().add(valueStorage.getCustomerCustomValues())
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
        val code = valueStorage.getCoveVerifier() ?: return null
        val authToken = valueStorage.getAuthKey() ?: return null
        return Authorization(code, authToken)
    }

    private suspend fun ChatBuilder.awaitBuild(): Chat = suspendCancellableCoroutine { continuation ->
        val cancellable: Cancellable = build(continuation::resume)
        continuation.invokeOnCancellation { cancellable.cancel() }
    }
}
