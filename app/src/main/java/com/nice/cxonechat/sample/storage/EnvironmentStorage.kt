package com.nice.cxonechat.sample.storage

import com.nice.cxonechat.enums.CXOneEnvironment
import com.nice.cxonechat.sample.storage.EnvironmentStorage.SelectedEnvironment.AU1
import com.nice.cxonechat.sample.storage.EnvironmentStorage.SelectedEnvironment.CA1
import com.nice.cxonechat.sample.storage.EnvironmentStorage.SelectedEnvironment.CUSTOM
import com.nice.cxonechat.sample.storage.EnvironmentStorage.SelectedEnvironment.EU1
import com.nice.cxonechat.sample.storage.EnvironmentStorage.SelectedEnvironment.JP1
import com.nice.cxonechat.sample.storage.EnvironmentStorage.SelectedEnvironment.NA1
import com.nice.cxonechat.sample.storage.EnvironmentStorage.SelectedEnvironment.UK1
import com.nice.cxonechat.sample.storage.ValueStorage.StringKey
import kotlinx.coroutines.flow.firstOrNull
import com.nice.cxonechat.state.Environment as SdkEnvironment

internal object EnvironmentStorage {

    suspend fun ValueStorage.getEnvironment(): SdkEnvironment? {
        val environmentName = getString(StringKey.ENVIRONMENT_USED_NAME_KEY).firstOrNull() ?: return null
        val selectedEnvironment = SelectedEnvironment.fromName(environmentName)
        return when (selectedEnvironment) {
            NA1 -> CXOneEnvironment.NA1.value
            EU1 -> CXOneEnvironment.EU1.value
            AU1 -> CXOneEnvironment.AU1.value
            CA1 -> CXOneEnvironment.CA1.value
            UK1 -> CXOneEnvironment.UK1.value
            JP1 -> CXOneEnvironment.JP1.value
            CUSTOM -> {
                val json = getString(StringKey.ENVIRONMENT_SERIALIZED_KEY)
                    .firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: return null
                Environment.deserialize(json)
            }
        }
    }

    suspend fun ValueStorage.setEnvironment(environment: SdkEnvironment) {
        val selectedEnv = SelectedEnvironment.fromSdkEnv(environment)
        val json = Environment.serialize(environment)
        setStrings(
            mapOf(
                StringKey.ENVIRONMENT_USED_NAME_KEY to selectedEnv.envName,
                StringKey.ENVIRONMENT_SERIALIZED_KEY to json
            )
        )
    }

    private enum class SelectedEnvironment(val envName: String) {
        NA1(CXOneEnvironment.NA1.name),
        EU1(CXOneEnvironment.EU1.name),
        AU1(CXOneEnvironment.AU1.name),
        CA1(CXOneEnvironment.CA1.name),
        UK1(CXOneEnvironment.UK1.name),
        JP1(CXOneEnvironment.JP1.name),

        CUSTOM("Sample_custom_env");

        companion object {
            fun fromSdkEnv(env: SdkEnvironment): SelectedEnvironment = fromName(env.name)

            fun fromName(name: String): SelectedEnvironment = when (name) {
                NA1.envName -> NA1
                EU1.envName -> EU1
                AU1.envName -> AU1
                CA1.envName -> CA1
                UK1.envName -> UK1
                JP1.envName -> JP1
                else -> CUSTOM
            }
        }
    }
}
