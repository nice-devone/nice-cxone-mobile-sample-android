package com.nice.cxonechat.sample.storage

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.sample.storage.Environment.Companion.toSdkEnvironment

internal data class SdkConfiguration(
    @SerializedName("name")
    val name: String,
    @SerializedName("environment")
    val environment: Environment,
    @SerializedName("brandId")
    val brandId: Long,
    @SerializedName("channelId")
    val channelId: String,
) {
    fun toSocketFactoryConfiguration() = SocketFactoryConfiguration(
        environment = environment.toSdkEnvironment(),
        brandId = brandId,
        channelId = channelId
    )
}
