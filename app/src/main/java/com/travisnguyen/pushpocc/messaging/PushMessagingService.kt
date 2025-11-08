package com.travisnguyen.pushpocc.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.travisnguyen.pushpocc.MainActivity
import com.travisnguyen.pushpocc.R
import com.travisnguyen.pushpocc.data.MessageStore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        MessageStore.saveToken(applicationContext, token)
        broadcastToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val notification = message.notification
        val title = notification?.title ?: message.data["title"] ?: "FCM message"
        val body = notification?.body ?: message.data["body"] ?: "Payload received"
        val data = message.data

        Log.d(TAG, "Message received: title=$title, body=$body, data=$data")
        MessageStore.appendMessage(applicationContext, title, body, data)
        broadcastMessage()
        showToast(body)
        showNotification(title, body, data)
    }

    private fun showToast(body: String) {
        Handler(Looper.getMainLooper()).post {
            android.widget.Toast.makeText(applicationContext, body, android.widget.Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        createNotificationChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
            if (data.isNotEmpty()) {
                putExtra("payload", HashMap(data))
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n$data"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Push PoC",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "FCM demo notifications"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun broadcastToken(token: String) {
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
            Intent(ACTION_TOKEN_REFRESHED).putExtra(EXTRA_TOKEN, token)
        )
    }

    private fun broadcastMessage() {
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
            Intent(ACTION_NEW_MESSAGE)
        )
    }

    companion object {
        const val ACTION_TOKEN_REFRESHED = "com.example.pushpoc.TOKEN_REFRESHED"
        const val ACTION_NEW_MESSAGE = "com.example.pushpoc.NEW_MESSAGE"
        const val EXTRA_TOKEN = "extra_token"
        private const val CHANNEL_ID = "push_poc_channel"
        private const val TAG = "PushMessagingSvc"
    }
}
