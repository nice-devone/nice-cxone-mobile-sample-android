package com.customerdynamics.sdk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.authorization.*
import com.amazon.identity.auth.device.api.workflow.RequestContext
import com.customerdynamics.androidlibrary.CXOneChat
import com.customerdynamics.sdk.service.PreferencesService


class LoginActivity : AppCompatActivity() {
    private var guestButton: Button? = null
    private var loginWithAmazonButton: ImageButton? = null
    private var requestContext: RequestContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        requestContext = RequestContext.create(this)

        guestButton = findViewById(R.id.guest_button)
        loginWithAmazonButton = findViewById(R.id.login_with_amazon)

        guestButton!!.setOnClickListener {
            showHome()
        }

        loginWithAmazonButton!!.setOnClickListener {
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
        PreferencesService.clearPreferences(this)
    }

    override fun onResume() {
        super.onResume()
        requestContext!!.onResume()
    }

    private fun showHome() {
        val intent = Intent(this, Home::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}