package com.nice.cxonechat.sample.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.Preferences.Key
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ValueStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val Context.storage: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_FILE_NAME)
    private val data: Flow<Preferences> by lazy { context.storage.data }

    fun getString(key: StringKey): Flow<String> {
        return data.map { preferences: Preferences ->
            preferences[key.value].orEmpty()
        }
    }


    suspend fun setString(key: StringKey, value: String) {
        context.storage.edit { preferences ->
            preferences[key.value] = value
        }
    }

    suspend fun setStrings(
        map: Map<StringKey, String>,
    ) {
        context.storage.edit { preferences ->
            for ((key, value) in map) {
                preferences[key.value] = value
            }
        }
    }

    suspend fun clear() {
        context.storage.edit(MutablePreferences::clear)
    }

    private companion object {
        private const val PREFERENCES_FILE_NAME = "com.nice.cxonechat.sample.settings"

        private const val PREF_CODE_VERIFIER_ID: String = "share_code_verifier"
        private const val PREF_AUTH_TOKEN: String = "share_auth_token"
        private const val PREF_ENVIRONMENT_NAME: String = "share_environment_name"
        private const val PREF_ENVIRONMENT_SERIALIZED: String = "share_custom_environment_serialized"
        private const val PREF_CHANNEL_ID: String = "share_channel_id"
        private const val PREF_BRAND_ID: String = "share_brand_id"
        private const val PREF_FIRST_NAME: String = "share_first_name"
        private const val PREF_LAST_NAME: String = "share_last_name"
    }

    enum class StringKey(val value: Key<String>) {
        CODE_VERIFIER_KEY(stringPreferencesKey(PREF_CODE_VERIFIER_ID)),
        AUTH_TOKEN_KEY(stringPreferencesKey(PREF_AUTH_TOKEN)),
        ENVIRONMENT_USED_NAME_KEY(stringPreferencesKey(PREF_ENVIRONMENT_NAME)),
        ENVIRONMENT_SERIALIZED_KEY(stringPreferencesKey(PREF_ENVIRONMENT_SERIALIZED)),
        CHANNEL_ID_KEY(stringPreferencesKey(PREF_CHANNEL_ID)),
        BRAND_ID_KEY(stringPreferencesKey(PREF_BRAND_ID)),
        FIRST_NAME_KEY(stringPreferencesKey(PREF_FIRST_NAME)),
        LAST_NAME_KEY(stringPreferencesKey(PREF_LAST_NAME));
    }
}
