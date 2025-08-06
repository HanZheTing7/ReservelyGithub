package com.example.reservely.presentation.features.edit_event

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.reservely.data.model.Event
import com.example.reservely.data.model.ParticipantInfo
import com.example.reservely.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditEventViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    var event by mutableStateOf<Event?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var joinAsParticipant by mutableStateOf(true)
        private set

    fun checkJoinAsParticipant(eventId: String) {
        val currentUser = auth.currentUser ?: return

        firestore.collection("events")
            .document(eventId)
            .collection("participants")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                joinAsParticipant = doc.exists()
            }
    }

    fun updateEvent(
        context: Context,
        event: Event,
        updatedEvent: Event,
        joinAsParticipant: Boolean,
        hostName: String,
        userData: UserProfile,
        onSuccess: () -> Unit
    ) {
        val currentUser = auth.currentUser ?: return

        val eventRef = firestore.collection("events").document(event.id)
        val participantsRef = eventRef.collection("participants")

        participantsRef.get().addOnSuccessListener { snapshot ->
            val currentCount = snapshot.size()
            val isAlreadyParticipant = snapshot.documents.any { it.id == currentUser.uid }

            if (joinAsParticipant && !isAlreadyParticipant && currentCount >= updatedEvent.maxPeople) {
                Toast.makeText(context, "Event is full. You cannot join as a participant.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            eventRef.set(updatedEvent)
                .addOnSuccessListener {
                    val participantDoc = participantsRef.document(currentUser.uid)

                    if (joinAsParticipant) {
                        if (!isAlreadyParticipant) {
                            val participant = ParticipantInfo(
                                userId = currentUser.uid,
                                userName = hostName,
                                profileImageUrl = userData.profileImageUrl ?: "",
                                joinedAt = Timestamp.now()
                            )
                            participantDoc.set(participant)
                        }
                    } else {
                        if (isAlreadyParticipant) {
                            participantDoc.delete()
                        }
                    }

                    Toast.makeText(context, "Event updated", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to update event", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun loadEvent(eventId: String) {
        isLoading = true
        firestore.collection("events")
            .document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                event = doc.toObject(Event::class.java)?.copy(id = doc.id)
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }
}
