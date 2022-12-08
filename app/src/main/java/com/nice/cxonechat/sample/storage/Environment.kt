package com.nice.cxonechat.sample.storage

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.state.Environment as SdkEnvironment

/**
 * Serializable version [SdkEnvironment].
 */
internal data class Environment(
    @SerializedName("name")
    override val name: String,
    @SerializedName("location")
    override val location: String,
    @SerializedName("baseUrl")
    override val baseUrl: String,
    @SerializedName("socketUrl")
    override val socketUrl: String,
    @SerializedName("originHeader")
    override val originHeader: String,
    @SerializedName("chatUrl")
    override val chatUrl: String,
) : SdkEnvironment() {
    internal companion object {
        fun deserialize(json: String): SdkEnvironment = Gson()
            .fromJson(json, Environment::class.java)
            .toSdkEnvironment()

        fun serialize(sdkEnvironment: SdkEnvironment): String {
            val environment = sdkEnvironment.toEnvironment()
            return Gson().toJson(environment)
        }

        private fun SdkEnvironment.toEnvironment() = Environment(
            name = name,
            location = location,
            baseUrl = baseUrl,
            socketUrl = socketUrl,
            originHeader = originHeader,
            chatUrl = chatUrl,
        )

        private fun Environment.toSdkEnvironment() = this
    }
}
