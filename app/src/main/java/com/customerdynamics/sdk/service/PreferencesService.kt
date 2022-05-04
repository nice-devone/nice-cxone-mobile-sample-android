package com.customerdynamics.sdk.service

import android.content.Context
import android.content.SharedPreferences
import java.util.*

class PreferencesService {

    companion object {
        private var PREF_CONSUMER_ID: String = "share_consumer_id"
        private var PREF_AUTH_TOKEN: String = "share_auth_token"
        private var PREF_VISITOR_ID: String = "share_visitor_id"

        private var sharedPreferences: SharedPreferences? = null

        private fun init(context: Context) {
            if (sharedPreferences == null) {
                sharedPreferences = context.getSharedPreferences(context.packageName, 0)
            }
        }

        fun clearPreferences(context: Context) {
            init(context)
            sharedPreferences?.edit()?.clear()?.apply()
        }

        fun setConsumerId(context: Context, consumerId: String) {
            init(context)
            val editor = sharedPreferences?.edit()
            editor?.putString(PREF_CONSUMER_ID, consumerId)
            editor?.apply()
        }

        fun getConsumerId(context: Context): String {
            init(context)
            return sharedPreferences?.getString(PREF_CONSUMER_ID, "")!!
        }

        fun setAuthToken(context: Context, token: String) {
            init(context)
            val editor = sharedPreferences?.edit()
            editor?.putString(PREF_AUTH_TOKEN, token)
            editor?.apply()
        }

        fun getAuthToken(context: Context): String {
            init(context)
            return sharedPreferences?.getString(PREF_AUTH_TOKEN, "")!!
        }

        fun setVisitorId(context: Context, visitorId: String) {
            init(context)
            val editor = sharedPreferences?.edit()
            editor?.putString(PREF_VISITOR_ID, visitorId)
            editor?.apply()
        }

        fun getVisitorId(context: Context): String {
            init(context)
            return sharedPreferences?.getString(PREF_VISITOR_ID, "")!!
        }
    }
}