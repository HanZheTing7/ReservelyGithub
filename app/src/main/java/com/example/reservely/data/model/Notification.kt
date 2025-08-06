package com.example.reservely.data.model

import com.google.firebase.Timestamp

data class Notification(
    val  title: String = "",
    val  message: String = "",
    val  timestamp: Long = System.currentTimeMillis(),
    val  type: String = "",
    val  eventId: String = "",
    val  isRead: Boolean = false,
    val expiry: Timestamp = Timestamp.now()

)


