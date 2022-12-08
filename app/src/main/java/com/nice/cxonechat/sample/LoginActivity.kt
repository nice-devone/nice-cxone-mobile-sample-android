package com.nice.cxonechat.sample

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.authorization.AuthCancellation
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult
import com.amazon.identity.auth.device.api.authorization.ProfileScope
import com.amazon.identity.auth.device.api.workflow.RequestContext
import com.nice.cxonechat.sample.databinding.ActivityLoginBinding
import com.nice.cxonechat.sample.domain.ChatRepository
import com.nice.cxonechat.sample.storage.ChatConfigurationStorage.getConfiguration
import com.nice.cxonechat.sample.storage.EnvironmentStorage.getEnvironment
import com.nice.cxonechat.sample.storage.ValueStorage
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.AUTH_TOKEN_KEY
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.CODE_VERIFIER_KEY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var dialog: ProgressDialog

    private lateinit var requestContext: RequestContext

    @Inject
    internal lateinit var valueStorage: ValueStorage

    @Inject
    internal lateinit var chatRepository: ChatRepository

    @Inject
    internal lateinit var valueStoreage: ValueStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestContext = RequestContext.create(this as Context)
        dialog = ProgressDialog(this)
        dialog.show()
        launchWithDialog(::login)
    }

    override fun onResume() {
        super.onResume()
        requestContext.onResume()
    }

    private suspend fun setLoginWithAmazon() {
        val codeVerifier = generateCodeVerifier()
        valueStoreage.setString(CODE_VERIFIER_KEY, codeVerifier)
        val loginWithAmazon = binding.loginWithAmazon
        loginWithAmazon.visibility = View.VISIBLE
        loginWithAmazon.setOnClickListener {
            val codeChallenge = generateCodeChallenge(codeVerifier)
            AuthorizationManager.authorize(
                AuthorizeRequest.Builder(requestContext)
                    .addScopes(ProfileScope.profile(), ProfileScope.postalCode())
                    .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
                    .withProofKeyParameters(codeChallenge, "S256")
                    .build()
            )
        }
        val authToken = requestContext.awaitAuthorization(
            onAuthError = { ae ->
                Log.i("amazon", ae.message.toString())
            },
            onAuthCancel = { cancellation ->
                Log.i("amazon", cancellation.description)
            }
        )
        valueStoreage.setString(AUTH_TOKEN_KEY, authToken)
        startChatActivity()
    }

    private fun setLoginAsGuest() {
        with(binding) {
            guestButton.visibility = View.VISIBLE
            guestButton.setOnClickListener { startChatActivity() }
        }
    }

    private fun startChatActivity() {
        launchWithDialog(chatRepository::createChat) {
            val intent = Intent(this, MainActivity::class.java).apply {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    /**
     * This is a workaround for special need of this sample application, where we don't know in advance,
     * if the application should use OAuth, so we need [com.nice.cxonechat.Chat] instance to determine said requirement,
     * based on channel configuration.
     * This shouldn't be a typical case for common applications.
     */
    private suspend fun login() {
        val environment = valueStorage.getEnvironment()
        val config = valueStorage.getConfiguration(environment)
        if (config == null) {
            finish()
        }
        val chat = chatRepository.createChat(skipAuthorization = true)
        val isAuthorizationEnabled = chat.configuration.isAuthorizationEnabled

        if (isAuthorizationEnabled) {
            setLoginWithAmazon()
        } else {
            setLoginAsGuest()
        }
    }

    private fun launchWithDialog(
        block: suspend () -> Any,
        blockAfterDialog: () -> Unit = {},
    ) {
        lifecycleScope.launch {
            dialog.show()
            block()
            dialog.hide()
            blockAfterDialog()
        }
    }

    private suspend fun RequestContext.awaitAuthorization(
        onAuthError: (AuthError) -> Unit,
        onAuthCancel: (AuthCancellation) -> Unit,
    ): String = suspendCancellableCoroutine { continuation ->
        val listener = object : AuthorizeListener() {
            override fun onSuccess(result: AuthorizeResult) {
                continuation.resumeWith(Result.success(result.authorizationCode))
            }

            override fun onError(ae: AuthError) = onAuthError(ae)

            override fun onCancel(cancellation: AuthCancellation) = onAuthCancel(cancellation)
        }
        continuation.invokeOnCancellation { unregisterListener(listener) }
        registerListener(listener)
    }
}
