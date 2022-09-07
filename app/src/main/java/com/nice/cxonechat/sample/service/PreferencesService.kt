package com.nice.cxonechat.sample.service

import android.content.Context
import android.content.SharedPreferences
import com.nice.cxonechat.services.Preferences

class PreferencesService {

    companion object {
        private var PREF_CONSUMER_ID: String = "share_consumer_id"
        private var PREF_AUTH_TOKEN: String = "share_auth_token"
        private var PREF_VISITOR_ID: String = "share_visitor_id"
        private var PREF_ENVIRONMENT: String = "share_environment"
        private var PREF_CHANNEL_ID: String = "share_channel_id"
        private var PREF_BRAND_ID: String = "share_brand_id"
        private var PREF_FIRST_NAME: String = "share_first_name"
        private var PREF_LAST_NAME: String = "share_last_name"

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

        fun getConsumerId(context: Context): String {
            init(context)
            return sharedPreferences?.getString(PREF_CONSUMER_ID, "")!!
        }

        fun setEnvironment(context: Context, environment: String) {
            init(context)
            val editor = sharedPreferences?.edit()
            editor?.putString(PREF_ENVIRONMENT, environment)
            editor?.apply()
        }

        fun getEnvironment(context: Context): String {
            init(context)
            return sharedPreferences?.getString(PREF_ENVIRONMENT, "")!!
        }

        fun setChannelId(context: Context, channelId: String) {
            init(context)
            val editor = sharedPreferences?.edit()
            editor?.putString(PREF_CHANNEL_ID, channelId)
            editor?.apply()
        }

        fun getChannelId(context: Context): String {
            init(context)
            return sharedPreferences?.getString(PREF_CHANNEL_ID, "")!!
        }

        fun setBrandId(context: Context, brandId: String) {
            init(context)
            val editor = sharedPreferences?.edit()
            editor?.putString(PREF_BRAND_ID, brandId)
            editor?.apply()
        }

        fun getBrandId(context: Context): String {
            init(context)
            return sharedPreferences?.getString(PREF_BRAND_ID, "")!!
        }

        fun setFirstName(context: Context, firstName: String) {
            init(context)
            val editor = sharedPreferences?.edit()
            editor?.putString(PREF_FIRST_NAME, firstName)
            editor?.apply()
        }

        fun getFirstName(context: Context): String {
            init(context)
            return sharedPreferences?.getString(PREF_FIRST_NAME, "")!!
        }

        fun setLastName(context: Context, lastName: String) {
            init(context)
            val editor = sharedPreferences?.edit()
            editor?.putString(PREF_LAST_NAME, lastName)
            editor?.apply()
        }

        fun getLastName(context: Context): String {
            init(context)
            return sharedPreferences?.getString(PREF_LAST_NAME, "")!!
        }
    }
}