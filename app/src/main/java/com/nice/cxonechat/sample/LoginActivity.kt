package com.nice.cxonechat.sample

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.authorization.*
import com.amazon.identity.auth.device.api.workflow.RequestContext
import com.nice.cxonechat.CXOneChat
import com.nice.cxonechat.models.channel.ChannelConfiguration
import com.nice.cxonechat.sample.CustomConfigurationFragment.Companion.BRAND_ID
import com.nice.cxonechat.sample.CustomConfigurationFragment.Companion.CHANNEL_ID
import com.nice.cxonechat.sample.CustomConfigurationFragment.Companion.CONFIGURATION
import com.nice.cxonechat.sample.HomeConfigurationFragment.Companion.ENVIRONMENT_SELECTED
import com.nice.cxonechat.sample.databinding.ActivityLoginBinding
import com.nice.cxonechat.sample.service.PreferencesService

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var requestContext: RequestContext? = null
    private var dialog: ProgressDialog? = null
    private val environmentSelected: String? by lazy {
        intent.extras?.getString(ENVIRONMENT_SELECTED)
    }
    private val brandId: String? by lazy { intent.extras?.getString(BRAND_ID) }
    private val channelId: String? by lazy { intent.extras?.getString(CHANNEL_ID) }
    private val configuration: ChannelConfiguration by lazy {
        intent.extras?.getSerializable(CONFIGURATION) as ChannelConfiguration
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestContext = RequestContext.create(this)
        dialog = ProgressDialog(this)

        if (configuration.isAuthorizationEnabled) {
            setLoginWithAmazon()
        } else {
            setLoginAsGuest()
        }
    }

    override fun onResume() {
        super.onResume()
        requestContext!!.onResume()
    }

    private fun setLoginWithAmazon() {
        with(binding) {
            loginWithAmazon.visibility = View.VISIBLE
            loginWithAmazon.setOnClickListener {
                val codeVerifier = generateCodeVerifier()
                val codeChallenge = generateCodeChallenge(codeVerifier)
                CXOneChat.setCodeVerifier(codeVerifier)
                AuthorizationManager.authorize(
                    AuthorizeRequest.Builder(requestContext)
                        .addScopes(ProfileScope.profile(), ProfileScope.postalCode())
                        .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
                        .withProofKeyParameters(codeChallenge, "S256")
                        .build()
                )
            }

            requestContext!!.registerListener(object : AuthorizeListener() {
                override fun onSuccess(result: AuthorizeResult) {
                    PreferencesService.setAuthToken(this@LoginActivity, result.authorizationCode)
                    CXOneChat.setAuthCode(result.authorizationCode)
                    showHome()
                }

                override fun onError(ae: AuthError) {
                    Log.i("amazon", ae.message.toString())
                }

                override fun onCancel(cancellation: AuthCancellation) {
                    Log.i("amazon", cancellation.description)
                }
            })
        }
    }

    private fun setLoginAsGuest() {
        with(binding) {
            guestButton.visibility = View.VISIBLE
            guestButton.setOnClickListener {
                showHome()
            }
        }
    }

    private fun showHome() {
        PreferencesService.setEnvironment(this, environmentSelected.orEmpty())
        PreferencesService.setChannelId(this, channelId.orEmpty())
        PreferencesService.setBrandId(this, brandId.orEmpty())
        val intent = Intent(this, HomeActivity::class.java).apply {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(ENVIRONMENT_SELECTED, environmentSelected)
            putExtra(BRAND_ID, brandId)
            putExtra(CHANNEL_ID, channelId)
        }
        startActivity(intent)
    }
}