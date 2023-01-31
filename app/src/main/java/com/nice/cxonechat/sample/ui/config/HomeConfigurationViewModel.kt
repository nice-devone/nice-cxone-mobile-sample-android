package com.nice.cxonechat.sample.ui.config

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.sample.storage.ChatConfigurationStorage.setConfiguration
import com.nice.cxonechat.sample.storage.SdkConfigurations
import com.nice.cxonechat.sample.storage.ValueStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@SuppressLint(
    "StaticFieldLeak" // Using ApplicationContext
)
@HiltViewModel
internal class HomeConfigurationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val valueStorage: ValueStorage,
) : ViewModel() {

    suspend fun getAssetConfigurations(): SdkConfigurations? = withContext(Dispatchers.Default) {
        context.assets.runCatching {
            open("environment/environment.json")
                .use {
                    SdkConfigurations.deserialize(it.bufferedReader())
                }
        }.getOrNull()
    }

    suspend fun setConfiguration(configuration: SocketFactoryConfiguration) {
        valueStorage.setConfiguration(configuration)
    }
}
