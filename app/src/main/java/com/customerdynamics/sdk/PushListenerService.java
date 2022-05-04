package com.customerdynamics.sdk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Objects;

import androidx.core.app.NotificationCompat;

public class PushListenerService extends FirebaseMessagingService {
    public static final String TAG = PushListenerService.class.getSimpleName();

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        Log.d(TAG, "Registering push notifications token: " + token);
        Objects.requireNonNull(Home.Companion.getPinpointManager(getApplicationContext())).getNotificationClient().registerDeviceToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
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
