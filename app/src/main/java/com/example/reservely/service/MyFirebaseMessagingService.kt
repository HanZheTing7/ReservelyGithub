package com.example.reservely.service

import com.example.reservely.notification.FcmTokenManager
import com.example.reservely.notification.NotificationHelperEntryPoint
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.EntryPointAccessors

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "Reservely"
        val body = remoteMessage.notification?.body ?: ""
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationHelperEntryPoint::class.java
        )

        val helper = entryPoint.notificationHelper()
        helper.showLocalNotification(applicationContext, title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenManager.saveToken(applicationContext, token)


        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationHelperEntryPoint::class.java
        )

        val helper = entryPoint.notificationHelper()
        helper.uploadFcmTokenIfNeeded(applicationContext)
    }
}
