package com.nice.cxonechat.sample

import android.app.Application
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import com.nice.cxonechat.sample.storage.TemporaryFileStorage
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SampleApplication : Application() {

    @Inject
    internal lateinit var temporaryFileStorage: TemporaryFileStorage

    override fun onCreate() {
        super.onCreate()
        /*
         SampleApp is using a bundled version of an emoji support library,
         for better support of the latest emojis on clean emulator instances.
         Clean emulator instances require setup & time in order to properly
         load the latest fonts which support current emojis.

         For usage on real devices, the DefaultEmojiCompatConfig is a better option,
         since it will download the font which can be updated without the need to
         update the bundled artifact.
         */
        EmojiCompat.init(BundledEmojiCompatConfig(this))
        clearStorage()
    }

    private fun clearStorage() {
        Thread {
            temporaryFileStorage.clear()
        }.start()
    }
}
