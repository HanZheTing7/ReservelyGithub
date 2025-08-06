package com.example.reservely.presentation.features.explore

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reservely.di.IoDispatcher
import com.example.reservely.data.model.Event
import com.example.reservely.data.model.ParticipantInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class CalendarUiState(
    val today: LocalDate,
    val selectedDate: LocalDate,
    val selectedHour: Int = -1  // -1 means "all hours"
)


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _today = mutableStateOf<LocalDate?>(null)
    val today: State<LocalDate?> = _today

    private val _selectedDate = mutableStateOf<LocalDate?>(null)
    val selectedDate: State<LocalDate?> = _selectedDate

    private val _weekOffset = mutableStateOf(0)
    val weekOffset: State<Int> = _weekOffset

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private val _participantMap = MutableStateFlow<Map<String, List<ParticipantInfo>>>(emptyMap())
    val participantMap: StateFlow<Map<String, List<ParticipantInfo>>> = _participantMap

    private val _selectedHour = mutableStateOf(-1)
    val selectedHour: State<Int> = _selectedHour

    private val _hostNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val hostNameMap: StateFlow<Map<String, String>> = _hostNameMap

    init {
        fetchFirebaseServerDate()
        loadEvents()
    }

    fun observeHostNamesFor(events: List<Event>) {
        events.map { it.hostId }
            .filter { it.isNotBlank() } // only valid host IDs
            .distinct()
            .forEach { hostId ->
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

    private fun fetchFirebaseServerDate() {
        val docRef = firestore.collection("server-time").document("now")

        // Update this doc in Firestore to refresh timestamp
        docRef.set(mapOf("timestamp" to FieldValue.serverTimestamp()))
            .continueWithTask {
                docRef.get()
            }.addOnSuccessListener { snapshot ->
                val timestamp = snapshot.getTimestamp("timestamp")?.toDate()

                val malaysiaDate = timestamp?.toInstant()
                    ?.atZone(ZoneId.of("Asia/Kuala_Lumpur"))
                    ?.toLocalDate()

                malaysiaDate?.let {
                    _today.value = it
                    _selectedDate.value = it
                }
            }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }


    private fun loadEvents() {
        firestore.collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }
                _events.value = list

                // Load participants after events are loaded
                loadAllParticipants(list)
            }
    }

    private fun loadAllParticipants(events: List<Event>) {
        viewModelScope.launch(ioDispatcher) {
            val db = FirebaseFirestore.getInstance()
            val newMap = mutableMapOf<String, List<ParticipantInfo>>()

            coroutineScope {
                val tasks = events.map { event ->
                    async {
                        try {
                            val result = db.collection("events")
                                .document(event.id)
                                .collection("participants")
                                .get()
                                .await()

                            val participants = result.documents.mapNotNull { doc ->
                                doc.toObject(ParticipantInfo::class.java)
                            }

                            event.id to participants
                        } catch (e: Exception) {
                            event.id to emptyList()
                        }
                    }
                }

                val results = tasks.awaitAll()
                results.forEach { (eventId, participants) ->
                    newMap[eventId] = participants
                }

                _participantMap.value = newMap.toMap() // Set once after all data is collected
            }
        }
    }


    fun onHourSelected(hour: Int) {
        _selectedHour.value = if (_selectedHour.value == hour) -1 else hour
    }

    fun checkOnboardingStatus(onResult: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val hasDone = doc.getBoolean("hasCompletedOnboarding") ?: false
                onResult(hasDone)
            }
    }

    fun completeOnboarding() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("hasCompletedOnboarding", true)
    }


}

