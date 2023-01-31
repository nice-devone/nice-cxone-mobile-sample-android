package com.nice.cxonechat.sample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat.Builder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nice.cxonechat.sample.R.drawable
import com.nice.cxonechat.sample.R.mipmap
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.domain.ChatRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class PushListenerService : FirebaseMessagingService() {
    @JvmField
    @Inject
    var repository: ChatRepository? = null

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val chat = repository?.chatInstance
        if (chat == null) {
            Log.v(TAG, "No chat instance present, token not passed")
            return
        }
        chat.setDeviceToken(token)
        Log.d(TAG, "Registering push notifications token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message: " + remoteMessage.data)
        sendNotification(
            remoteMessage.data["pinpoint.notification.title"],
            remoteMessage.data["pinpoint.notification.body"])
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val channelId = getString(string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = Builder(this, channelId)
            .setSmallIcon(drawable.account_avatar)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setSmallIcon(mipmap.ic_launcher)
            .setAutoCancel(false)
            .setSound(defaultSoundUri)
            .setPriority(2)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(
            channelId,
            getString(string.notification_channel_title),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        val notification = notificationBuilder.build()
        notificationManager.notify(0 /* ID of notification */, notification)
    }

    companion object {
        val TAG: String = PushListenerService::class.java.simpleName

        @Suppress("Deprecation")
        fun getMessage(data: Bundle): String {
            return (data["data"] as HashMap<*, *>?).toString()
        }
    }
}
