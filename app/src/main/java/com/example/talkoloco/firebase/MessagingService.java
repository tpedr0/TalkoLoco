package com.example.talkoloco.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessagingService extends FirebaseMessagingService {

    /**
     * Called when a new FCM token is generated for this app instance.
     * Logs the new token for debugging purposes.
     *
     * @param token The new FCM token generated for this app
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "Token:" +token);
    }

    /**
     * Called when a new FCM message is received.
     * Logs the message body for debugging purposes.
     *
     * @param message The FCM message received containing notification data
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.d("FCM", "460 Message: " +message.getNotification().getBody());
    }
}
