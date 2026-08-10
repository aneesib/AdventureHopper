package com.anees.adventurehopper.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.anees.adventurehopper.data.FirebaseNotificationRepository

class FirebaseNotificationService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        FirebaseNotificationRepository(this).registerCurrentDeviceToken { }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "חשמלאי בכיס"
        val body = message.notification?.body ?: message.data["body"] ?: return
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel("service_requests", "בקשות שירות", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        manager.notify(
            body.hashCode(),
            NotificationCompat.Builder(this, "service_requests")
                .setSmallIcon(com.anees.adventurehopper.R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .build()
        )
    }
}