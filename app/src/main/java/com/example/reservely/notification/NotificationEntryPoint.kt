package com.example.reservely.notification

import com.example.reservely.util.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationHelperEntryPoint {
    fun notificationHelper(): NotificationHelper
}
