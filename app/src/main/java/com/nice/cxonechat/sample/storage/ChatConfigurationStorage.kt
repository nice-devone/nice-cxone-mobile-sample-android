package com.nice.cxonechat.sample.storage

import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.sample.storage.EnvironmentStorage.setEnvironment
import com.nice.cxonechat.state.Environment
import kotlinx.coroutines.flow.firstOrNull

internal object ChatConfigurationStorage {

    suspend fun ValueStorage.getConfiguration(
        environment: Environment?,
    ): SocketFactoryConfiguration? {
        if (environment == null) return null
        val brandId = getString(ValueStorage.StringKey.BRAND_ID_KEY)
            .firstOrNull()
            ?.toLongOrNull()
            ?: return null
        val channelId = getString(ValueStorage.StringKey.CHANNEL_ID_KEY).firstOrNull() ?: return null
        return SocketFactoryConfiguration(
            environment = environment,
            brandId = brandId,
            channelId = channelId,
        )
    }

    suspend fun ValueStorage.setConfiguration(configuration: SocketFactoryConfiguration) {
        setEnvironment(configuration.environment)
        val toStore = mapOf(
            ValueStorage.StringKey.BRAND_ID_KEY to configuration.brandId.toString(),
            ValueStorage.StringKey.CHANNEL_ID_KEY to configuration.channelId,
        )
        setStrings(toStore)
    }

}
