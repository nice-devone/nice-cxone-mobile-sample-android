package com.nice.cxonechat.sample.storage

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.AUTH_TOKEN_KEY
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.CODE_VERIFIER_KEY
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.CUSTOMER_CUSTOM_VALUES_KEY
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.FIRST_NAME_KEY
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey.LAST_NAME_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull


internal suspend fun ValueStorage.getCoveVerifier(): String? =
    getString(CODE_VERIFIER_KEY).firstNotBlankOrNull()

internal suspend fun ValueStorage.getAuthKey(): String? =
    getString(AUTH_TOKEN_KEY).firstNotBlankOrNull()

internal suspend fun ValueStorage.getUserFirstName(): String? =
    getString(FIRST_NAME_KEY).firstNotBlankOrNull()

internal suspend fun ValueStorage.getUserLastName(): String? =
    getString(LAST_NAME_KEY).firstNotBlankOrNull()

internal suspend fun ValueStorage.getCustomerCustomValues(): Map<String, String> {
    @Suppress("UNCHECKED_CAST")
    val parameterized = TypeToken.getParameterized(
        Map::class.java,
        String::class.java,
        String::class.java
    ) as? TypeToken<Map<String, String>>?
    val json = getString(CUSTOMER_CUSTOM_VALUES_KEY).firstNotBlankOrNull()
    return if (json != null) {
        Gson().fromJson(json, parameterized) ?: emptyMap()
    } else {
        emptyMap()
    }
}

internal fun <T> T.toJson(): String = Gson().toJson(this)


private suspend inline fun Flow<String>.firstNotBlankOrNull(): String? = firstOrNull()?.takeIf(String::isNotBlank)
