package com.example.reservely.presentation.features.notification


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reservely.data.model.Notification
import com.example.reservely.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(val firestore: FirebaseFirestore,
    val auth: FirebaseAuth, private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount


    init {
        listenToNotifications()
    }

    private fun listenToNotifications() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore
            .collection("notifications")
            .document(currentUserId)
            .collection("userNotifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Notifications", "Listen failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val notificationList = snapshot.documents.mapNotNull { doc ->
                    val raw = doc.data ?: return@mapNotNull null
                    Notification(
                        title = raw["title"] as? String ?: "",
                        message = raw["message"] as? String ?: "",
                        type = raw["type"] as? String ?: "",
                        eventId = raw["eventId"] as? String ?: "",
                        timestamp = (raw["timestamp"] as? Long) ?: 0L,
                        isRead = raw["isRead"] as? Boolean ?: false
                    )
                }

                _notifications.value = notificationList
                _unreadCount.value = notificationList.count { !it.isRead }

                Log.d("BadgeCount", "Updated unread count = ${_unreadCount.value} (isFromCache=${snapshot.metadata.isFromCache})")
            }
    }

    fun markAllAsRead(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        val userNotificationsRef = firestore
            .collection("notifications")
            .document(userId)
            .collection("userNotifications")

        viewModelScope.launch {
            try {
                val snapshot = userNotificationsRef.whereEqualTo("isRead", false).get().await()

                if (snapshot.isEmpty) {
                    Log.d("Notifications", "No unread notifications to mark.")
                    _unreadCount.value = 0
                    onSuccess()
                    return@launch
                }

                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }

                batch.commit().await()
                Log.d("Notifications", "SUCCESS: Marked all as read in Firestore.")

                _unreadCount.value = 0
                onSuccess()

            } catch (e: Exception) {
                Log.e("Notifications", "FAILED to mark as read: ${e.message}")
                onError(e)
            }
        }
    }

}
