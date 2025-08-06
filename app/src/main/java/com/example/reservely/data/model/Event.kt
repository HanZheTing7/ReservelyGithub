package com.example.reservely.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.io.Serializable

data class Event(val id: String = "",
                 val title: String = "",
                 val address: String = "",
                 val dateTime: Timestamp = Timestamp.now(), // Format: "2025-07-01 18:30"
                 val durationMinutes: Int = 60, // default 1 hour
                 val maxPeople: Int = 0,
                 val pricePerPerson: Double? = null,
                 val priceMale: Double? = null,
                 val priceFemale: Double? = null,
                 val isDualPrice: Boolean = false,
                 val description: String = "",
                 val category: String = "",
                 val hostId: String = "",
                 val hostName: String = "",
                 val courtBooked: Boolean = false,
                 val location: GeoPoint? = null,
                 val endTimeMillis: Long = 0L,
                 val chatFrozen: Boolean = false,
                 val expireAt: Timestamp? = null
                 ) : Serializable

data class JoinRequest(
    val userId: String = "",
    val userName: String = "",
    val gender: String = "",
    val age: Int = 0,
    val profileImageUrl: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class PlusOneRequest(
    val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterProfileImage: String = "",
    val friendName: String = "",
    val eventId: String = "",
    val timestamp: Timestamp = Timestamp.now(),
)


data class ParticipantInfo(
    val userId: String = "",
    val userName: String = "",
    val gender: String = "",
    val age: Int = 0,
    val profileImageUrl: String = "",
    val joinedAt: Timestamp = Timestamp.now()
)

data class EventWithParticipantCount(
    val event: Event,
    val participantCount: Int
)

data class SelectedPlace(
    val address: String,
    val latLng: LatLng
)