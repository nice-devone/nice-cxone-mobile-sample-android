package com.nice.cxonechat.sample.storage

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import java.io.Reader

internal data class SdkConfigurations(
    @SerializedName("configurations")
    val configurations: List<SdkConfiguration>,
) {
    companion object {
        fun deserialize(json: Reader): SdkConfigurations =
            Gson().fromJson(JsonReader(json), SdkConfigurations::class.java)
    }
}
