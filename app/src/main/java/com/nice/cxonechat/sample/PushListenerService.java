package com.nice.cxonechat.sample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nice.cxonechat.Chat;
import com.nice.cxonechat.sample.domain.ChatRepository;

import java.util.HashMap;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PushListenerService extends FirebaseMessagingService {
    public static final String TAG = PushListenerService.class.getSimpleName();

    @Inject
    ChatRepository repository;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Chat chat = repository.getChatInstance();
        if (chat == null) {
            Log.v(TAG, "No chat instance present, token not passed");
            return;
        }
        chat.setDeviceToken(token);
        Log.d(TAG, "Registering push notifications token: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message: " + remoteMessage.getData());
        sendNotification(
                remoteMessage.getData().get("pinpoint.notification.title"),
                remoteMessage.getData().get("pinpoint.notification.body"));
    }

    private void sendNotification(String title, String messageBody) {
        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.account_avatar)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setAutoCancel(false)
                        .setSound(defaultSoundUri)
                        .setPriority(2);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = notificationBuilder.build();

        notificationManager.notify(0 /* ID of notification */, notification);
    }

    public static String getMessage(Bundle data) {
        return ((HashMap) data.get("data")).toString();
    }
}