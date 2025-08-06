package com.example.reservely.presentation.features.joined_event

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reservely.data.model.Event
import com.example.reservely.data.model.EventWithParticipantCount
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class JoinedEventsViewModel @Inject constructor(
    val firestore: FirebaseFirestore,
    val auth: FirebaseAuth,
) : ViewModel() {

    private val _upcomingEvents = MutableStateFlow<List<EventWithParticipantCount>>(emptyList())
    val upcomingEvents: StateFlow<List<EventWithParticipantCount>> = _upcomingEvents

    private val _pastEvents = MutableStateFlow<List<EventWithParticipantCount>>(emptyList())
    val pastEvents: StateFlow<List<EventWithParticipantCount>> = _pastEvents

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _hostNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val hostNameMap: StateFlow<Map<String, String>> = _hostNameMap

    init {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("JoinedEventsViewModel", "User not logged in — currentUser is null")
        } else {
            Log.d("JoinedEventsViewModel", "User ID: $userId")
        }

        loadJoinedEvents()
    }

    fun observeHostNamesFor(events: List<Event>) {
        events.map { it.hostId }.distinct().forEach { hostId ->
            FirebaseFirestore.getInstance().collection("users")
                .document(hostId)
                .addSnapshotListener { snapshot, _ ->
                    val updatedName = snapshot?.getString("name") ?: return@addSnapshotListener
                    _hostNameMap.update { current ->
                        current + (hostId to updatedName)
                    }
                }
        }
    }

    fun loadJoinedEvents() {
        val userId = auth.currentUser?.uid ?: return


        viewModelScope.launch {
            try {
                _isLoading.value = true
                val now = Timestamp.now()

                val snapshot = firestore.collection("events").get().await()
                val events = snapshot.documents.mapNotNull {
                    it.toObject(Event::class.java)?.copy(id = it.id)
                }
                val upcomingList = mutableListOf<EventWithParticipantCount>()
                val pastList = mutableListOf<EventWithParticipantCount>()


                events.map { event ->
                    async {
                        val eventRef = firestore.collection("events").document(event.id)

                        val participantDoc =
                            eventRef.collection("participants").document(userId).get().await()
                        val participantCount =
                            eventRef.collection("participants").get().await().size()

                        val isParticipant = participantDoc.exists() || event.hostId == userId

                        if (isParticipant) {
                            val withCount = EventWithParticipantCount(event, participantCount)
                            if (event.dateTime > now) {
                                upcomingList.add(withCount)
                            } else {
                                pastList.add(withCount)
                            }
                        }
                    }
                }.awaitAll()

                _upcomingEvents.value = upcomingList.sortedBy { it.event.dateTime }
                _pastEvents.value = pastList.sortedByDescending { it.event.dateTime }

            } catch (e: Exception) {
                Log.e("JoinedEventsViewModel", "Error loading events: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }


}
