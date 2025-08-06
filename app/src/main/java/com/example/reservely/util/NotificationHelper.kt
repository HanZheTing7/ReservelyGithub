package com.example.reservely.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.reservely.R
import com.example.reservely.notification.FcmTokenManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class NotificationHelper @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    fun showLocalNotification(context: Context, title: String, message: String) {
        val channelId = "default_channel_id"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Event Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.therainz)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun uploadFcmTokenIfNeeded(context: Context) {
        val userId = auth.currentUser?.uid ?: return
        val localToken = FcmTokenManager.getToken(context) ?: return

        val userRef = firestore.collection("users").document(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            val firestoreToken = snapshot.getString("fcmToken")
            if (firestoreToken != localToken) {
                userRef.update("fcmToken", localToken)
                    .addOnSuccessListener {
                        Log.d("FCM", "Synced local FCM token to Firestore")
                    }
            }
        }
    }
}
