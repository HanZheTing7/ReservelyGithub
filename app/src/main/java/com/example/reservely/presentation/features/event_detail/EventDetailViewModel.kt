package com.example.reservely.presentation.features.event_detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reservely.presentation.features.chat.ChatHelper
import com.example.reservely.data.model.Event
import com.example.reservely.data.model.JoinRequest
import com.example.reservely.data.model.ParticipantInfo
import com.example.reservely.data.model.PlusOneRequest
import com.example.reservely.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
open class EventDetailViewModel@Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val chatHelper: ChatHelper,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _joinRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val joinRequests: StateFlow<List<JoinRequest>> = _joinRequests

    private val _withdrawRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val withdrawRequests: StateFlow<List<JoinRequest>> = _withdrawRequests

    private val _waitlistRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val waitlistRequests: StateFlow<List<JoinRequest>> = _waitlistRequests

    private val _participants = MutableStateFlow<List<ParticipantInfo>>(emptyList())
    val participants: StateFlow<List<ParticipantInfo>> = _participants

    private val _hasRequested = MutableStateFlow(false)
    val hasRequested: StateFlow<Boolean> = _hasRequested

    private val _hasJoined = savedStateHandle.getStateFlow("hasJoined", false)
    val hasJoined: StateFlow<Boolean> = _hasJoined

    private val _hasWithdrawRequested = MutableStateFlow(false)
    val hasWithdrawRequested: StateFlow<Boolean> = _hasWithdrawRequested

    private val _isWaitlisted = MutableStateFlow(false)
    val isWaitlisted: StateFlow<Boolean> = _isWaitlisted

    private val _isEventFull = MutableStateFlow(false)
    val isEventFull: StateFlow<Boolean> = _isEventFull

    private val _plusOneRequests = MutableStateFlow<List<PlusOneRequest>>(emptyList())
    val plusOneRequests: StateFlow<List<PlusOneRequest>> = _plusOneRequests

    private val _plusOneWaitlist = MutableStateFlow<List<PlusOneRequest>>(emptyList())
    val plusOneWaitlist: StateFlow<List<PlusOneRequest>> = _plusOneWaitlist

    private val _isLoadingUserStatus = MutableStateFlow(true)
    val isLoadingUserStatus: StateFlow<Boolean> = _isLoadingUserStatus

    private val _hostName = MutableStateFlow<String?>(null)
    val hostName: StateFlow<String?> = _hostName

    fun observeHostName(hostId: String, fallback: String = "Host") {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(hostId)
            .addSnapshotListener { snapshot, _ ->
                val name = snapshot?.getString("name")
                if (!name.isNullOrBlank()) {
                    _hostName.value = name
                } else {
                    _hostName.value = fallback
                }
            }
    }

    open fun sendNotification(
        toUserId: String,
        title: String,
        message: String,
        type: String,
        eventId: String
    ) {
        val data = hashMapOf(
            "toUserId" to toUserId,
            "title" to title,
            "message" to message,
            "type" to type,
            "eventId" to eventId
        )

        functions
            .getHttpsCallable("createNotification")
            .call(data)
            .addOnSuccessListener {
                Log.d("Notification", "Notification created successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Failed to create notification", e)
            }
    }

    fun loadJoinRequests(eventId: String) {
        firestore.collection("events").document(eventId)
            .collection("joinRequests")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _joinRequests.value = it.documents.mapNotNull { doc -> doc.toObject(JoinRequest::class.java) }
                }
            }
    }

    fun loadWithdrawalRequests(eventId: String) {
        firestore.collection("events").document(eventId)
            .collection("withdrawals")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _withdrawRequests.value = it.documents.mapNotNull { doc -> doc.toObject(
                        JoinRequest::class.java) }
                }
            }
    }

    fun loadWaitlistRequests(eventId: String) {
        firestore.collection("events").document(eventId)
            .collection("waitlist")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _waitlistRequests.value = it.documents.mapNotNull { doc -> doc.toObject(
                        JoinRequest::class.java) }
                }
            }
    }

    suspend fun loadParticipants(eventId: String) {
        try {
            val snapshot = firestore.collection("events")
                .document(eventId)
                .collection("participants")
                .get()
                .await()

            val participantList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ParticipantInfo::class.java)
            }
            _participants.value = participantList
        } catch (e: Exception) {
            _participants.value = emptyList()
        }
    }


    fun checkUserStatus(eventId: String, userId: String) {
        viewModelScope.launch {
            _isLoadingUserStatus.value = true

            val join = firestore.collection("events").document(eventId)
                .collection("joinRequests").document(userId).get().await()
            val joined = firestore.collection("events").document(eventId)
                .collection("participants").document(userId).get().await()
            val withdraw = firestore.collection("events").document(eventId)
                .collection("withdrawals").document(userId).get().await()
            val wait = firestore.collection("events").document(eventId)
                .collection("waitlist").document(userId).get().await()

            _hasRequested.value = join.exists()
            savedStateHandle["hasJoined"] = joined.exists()
            _hasWithdrawRequested.value = withdraw.exists()
            _isWaitlisted.value = wait.exists()

            _isLoadingUserStatus.value = false
        }
    }

    fun checkIfEventIsFull(event: Event) {
        firestore.collection("events").document(event.id)
            .collection("participants")
            .get()
            .addOnSuccessListener { snapshot ->
                _isEventFull.value = snapshot.size() >= event.maxPeople
            }
    }

    fun toggleJoinRequest(event: Event, userId: String, userName: String, onResult: (Boolean, String?) -> Unit) {
        val eventRef = firestore.collection("events").document(event.id)
        val joinRef = eventRef.collection("joinRequests").document(userId)
        val waitlistRef = eventRef.collection("waitlist").document(userId)

        viewModelScope.launch {
            try {
                val joinSnap = joinRef.get().await()
                val waitSnap = waitlistRef.get().await()

                if (joinSnap.exists()) {
                    joinRef.delete().await()
                    _hasRequested.value = false
                    onResult(true, "Join request cancelled.")
                    return@launch
                }

                if (waitSnap.exists()) {
                    waitlistRef.delete().await()
                    _isWaitlisted.value = false
                    onResult(true, "Waitlist cancelled.")
                    return@launch
                }

                // Fetch latest participants count
                val participantsSnap = eventRef.collection("participants").get().await()
                val isFull = participantsSnap.size() >= event.maxPeople

                // Fetch full user profile
                val userDoc = firestore.collection("users").document(userId).get().await()
                val userProfile = userDoc.toObject(UserProfile::class.java)

                val request = JoinRequest(
                    userId = userId,
                    userName = userProfile?.name ?: userName,
                    profileImageUrl = userProfile?.profileImageUrl ?: "",
                    gender = userProfile?.gender ?: "",
                    age = userProfile?.age ?: 0
                )

                if (isFull) {
                    waitlistRef.set(request).await()
                    _isWaitlisted.value = true
                    onResult(true, "Event is full. You've been added to the waitlist.")
                } else {
                    joinRef.set(request).await()
                    _hasRequested.value = true
                    sendNotification(
                        toUserId = event.hostId,
                        title = "Join Request",
                        message = "${request.userName} requested to join your event '${event.title}'",
                        type = "join_request",
                        eventId = event.id
                    )
                    onResult(true, "Join request sent.")
                }

            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to send join request.")
            }
        }
    }



    fun requestWithdrawal(event: Event, userId: String) {
        viewModelScope.launch {
            try {
                // Fetch user profile info
                val userDoc = firestore.collection("users").document(userId).get().await()
                val userProfile = userDoc.toObject(UserProfile::class.java)

                val withdrawalRequest = JoinRequest(
                    userId = userId,
                    userName = userProfile?.name ?: "",
                    profileImageUrl = userProfile?.profileImageUrl ?: "",
                    gender = userProfile?.gender ?: "",
                    age = userProfile?.age ?: 0
                )

                firestore.collection("events").document(event.id)
                    .collection("withdrawals").document(userId)
                    .set(withdrawalRequest).await()

                _hasWithdrawRequested.value = true

                // Send notification to host
                sendNotification(
                    toUserId = event.hostId,
                    title = "Withdrawal Request",
                    message = "${withdrawalRequest.userName} requested to withdraw from '${event.title}'",
                    type = "withdraw_request",
                    eventId = event.id
                )
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }



    fun cancelWithdrawal(eventId: String, userId: String) {
        firestore.collection("events").document(eventId)
            .collection("withdrawals").document(userId)
            .delete()
            .addOnSuccessListener {
                _hasWithdrawRequested.value = false
            }
    }

    fun acceptJoinRequest(event: Event, request: JoinRequest, onResult: (Boolean, String?) -> Unit) {
        val participantsRef = firestore.collection("events").document(event.id)
            .collection("participants").document(request.userId)
        val joinRef = firestore.collection("events").document(event.id)
            .collection("joinRequests").document(request.userId)

        viewModelScope.launch {
            // Ensure full participant profile (even if already provided in request)
            val userDoc = firestore.collection("users").document(request.userId).get().await()
            val userProfile = userDoc.toObject(UserProfile::class.java)

            val participant = ParticipantInfo(
                userId = request.userId,
                userName = userProfile?.name ?: request.userName,
                profileImageUrl = userProfile?.profileImageUrl ?: request.profileImageUrl,
                gender = userProfile?.gender ?: request.gender,
                age = userProfile?.age ?: request.age
            )

            participantsRef.set(participant).await()
            joinRef.delete().await()

            // Add to Stream Chat channel
            addMemberToChat(event.id, request.userId)


            sendNotification(
                toUserId = request.userId,
                title = "Request Accepted",
                message = "Your request to join '${event.title}' has been accepted.",
                type = "join_accepted",
                eventId = event.id
            )

            onResult(true, "Accepted.")
            loadParticipants(event.id) // Refresh list
        }
    }


    fun removeJoinRequest(eventId: String, userId: String) {
        firestore.collection("events").document(eventId)
            .collection("joinRequests").document(userId)
            .delete()
    }

    fun acceptWithdrawalRequest(event: Event, userId: String, onResult: (Boolean, String?) -> Unit) {
        val eventRef = firestore.collection("events").document(event.id)
        val participantRef = eventRef.collection("participants").document(userId)
        val withdrawalRef = eventRef.collection("withdrawals").document(userId)

        viewModelScope.launch {
            try {
                val participantSnap = participantRef.get().await()
                if (participantSnap.exists()) {
                    participantRef.delete().await()
                }
                withdrawalRef.delete().await()

                removeMemberFromChat(event.id, userId)

                sendNotification(
                    toUserId = userId,
                    title = "Withdrawal Accepted",
                    message = "You have been withdrawn from '${event.title}'.",
                    type = "withdraw_accepted",
                    eventId = event.id
                )

                onResult(true, "Withdraw accepted.")
                loadParticipants(event.id) // refresh list
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to accept withdrawal.")
            }
        }
    }


    fun rejectWithdrawalRequest(event: Event, userId: String, onResult: (Boolean, String?) -> Unit) {
        firestore.collection("events").document(event.id)
            .collection("withdrawals").document(userId)
            .delete()
            .addOnSuccessListener {
                sendNotification(
                    toUserId = userId,
                    title = "Withdrawal Rejected",
                    message = "Your request to withdraw from '${event.title}' was rejected.",
                    type = "withdraw_rejected",
                    eventId = event.id
                )

                onResult(true, "Withdrawal rejected.")
            }
    }

    fun acceptWaitlistRequest(event: Event, request: JoinRequest, onResult: (Boolean, String?) -> Unit) {
        val eventRef = firestore.collection("events").document(event.id)
        val participantsRef = eventRef.collection("participants")
        val waitlistRef = eventRef.collection("waitlist").document(request.userId)

        viewModelScope.launch {
            try {
                val participantsSnap = participantsRef.get().await()
                if (participantsSnap.size() >= event.maxPeople) {
                    onResult(false, "Event is full. Cannot accept more participants.")
                    return@launch
                }

                val userDoc = firestore.collection("users").document(request.userId).get().await()
                val userProfile = userDoc.toObject(UserProfile::class.java)

                val participant = ParticipantInfo(
                    userId = request.userId,
                    userName = userProfile?.name ?: request.userName,
                    profileImageUrl = userProfile?.profileImageUrl ?: request.profileImageUrl,
                    gender = userProfile?.gender ?: request.gender,
                    age = userProfile?.age ?: request.age
                )

                participantsRef.document(request.userId).set(participant).await()
                waitlistRef.delete().await()

                // Add to Stream Chat channel
                addMemberToChat(event.id, request.userId)


                sendNotification(
                    toUserId = request.userId,
                    title = "Request Accepted",
                    message = "Your request to join '${event.title}' has been accepted.",
                    type = "join_accepted",
                    eventId = event.id
                )

                onResult(true, "Waitlisted user accepted.")
                loadParticipants(event.id) // refresh list
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error accepting waitlisted user.")
            }
        }
    }

    // Load Plus One Requests
    fun loadPlusOneRequests(eventId: String) {
        firestore.collection("events").document(eventId)
            .collection("plusOnes")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val requests = snapshot.toObjects(PlusOneRequest::class.java)
                    _plusOneRequests.value = requests
                }
            }

        firestore.collection("events").document(eventId)
            .collection("plusOneWaitlist")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _plusOneWaitlist.value = it.documents.mapNotNull { doc -> doc.toObject(
                        PlusOneRequest::class.java) }
                }
            }
    }

    // User Request +1 (friendName is input by user)
    fun submitPlusOneRequest(event: Event, userId: String, fallbackUserName: String, friendName: String, onResult: (Boolean, String?) -> Unit) {
        val eventRef = firestore.collection("events").document(event.id)


        viewModelScope.launch {
            try {
                val participantsSnap = eventRef.collection("participants").get().await()
                val isFull = participantsSnap.size() >= event.maxPeople
                val userDoc = firestore.collection("users").document(userId).get().await()

                val userName = getUserName(userId)
                val userProfile = userDoc.toObject(UserProfile::class.java)

                val plusOneRef = eventRef.collection("plusOnes").document() // Auto-ID
                val waitlistRef = eventRef.collection("plusOneWaitlist").document() // Auto-ID


                val request = PlusOneRequest(
                    id = if (isFull) waitlistRef.id else plusOneRef.id,
                    requesterId = userId,
                    requesterName = userName,
                    friendName = friendName,
                    requesterProfileImage = userProfile?.profileImageUrl ?: "",
                    eventId = event.id,
                    timestamp = Timestamp.now()
                )

                if (isFull) {
                    Log.d("DEBUG", "PlusOneRequest payload: ${Gson().toJson(request)}")


                    waitlistRef.set(request).await()
                    onResult(true, "Event is full. Your +1 has been added to the waitlist.")
                } else {
                    plusOneRef.set(request).await()
                    sendNotification(
                        toUserId = event.hostId,
                        title = "+1 Request",
                        message = "$userName requested to add '$friendName' as +1 in '${event.title}'",
                        type = "plus_one_request",
                        eventId = event.id
                    )
                    onResult(true, "Your +1 request has been sent to the host.")
                }

            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to submit +1 request.")
            }
        }
    }

    // Host approves +1 request
    fun approvePlusOneRequest(event: Event, request: PlusOneRequest, isWaitlist: Boolean, onResult: (Boolean, String?) -> Unit) {
        val eventRef = firestore.collection("events").document(event.id)

        viewModelScope.launch {
            try {
                val participantsSnap = eventRef.collection("participants").get().await()
                if (participantsSnap.size() >= event.maxPeople) {
                    onResult(false, "Event is full. Cannot approve +1.")
                    return@launch
                }

                val requesterDoc = firestore.collection("users").document(request.requesterId).get().await()
                val requesterProfileImage = requesterDoc.getString("profileImageUrl") ?: ""

                val participant = ParticipantInfo(
                    userId = "${request.requesterId}_plusOne_${request.id}",
                    userName = "${request.friendName} (+1 of ${request.requesterName})",
                    profileImageUrl = requesterProfileImage,
                    age = 0,
                    gender = ""
                )

                // Add to participants
                eventRef.collection("participants")
                    .document(participant.userId)
                    .set(participant).await()

                // Remove from plusOnes or plusOneWaitlist
                val collectionName = if (isWaitlist) "plusOneWaitlist" else "plusOnes"
                eventRef.collection(collectionName)
                    .document(request.id)
                    .delete().await()

                sendNotification(
                    toUserId = request.requesterId,
                    title = "+1 Approved",
                    message = "Your +1 request for '${request.friendName}' has been approved.",
                    type = "plus_one_approved",
                    eventId = event.id
                )

                loadParticipants(event.id)
                onResult(true, "+1 request approved.")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error approving +1.")
            }
        }
    }

    // Host rejects +1 request
    fun rejectPlusOneRequest(eventId: String, request: PlusOneRequest, isWaitlist: Boolean, onResult: (Boolean, String?) -> Unit) {
        val collectionName = if (isWaitlist) "plusOneWaitlist" else "plusOnes"
        firestore.collection("events").document(eventId)
            .collection(collectionName).document(request.id)
            .delete()
            .addOnSuccessListener {
                sendNotification(
                    toUserId = request.requesterId,
                    title = "+1 Rejected",
                    message = "Your +1 request for '${request.friendName}' was rejected.",
                    type = "plus_one_rejected",
                    eventId = eventId
                )

                onResult(true, "+1 request rejected.")
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    // Host adds +1 directly
    fun addHostPlusOne(event: Event, friendName: String, onResult: (Boolean, String?) -> Unit) {
        val eventRef = firestore.collection("events").document(event.id)

        viewModelScope.launch {
            try {
                Log.d("AddPlusOne", "HostId: ${event.hostId}, CurrentUser: ${FirebaseAuth.getInstance().currentUser?.uid}")
                val participantsSnap = eventRef.collection("participants").get().await()
                if (participantsSnap.size() >= event.maxPeople) {
                    onResult(false, "Event is currently full.")
                    return@launch
                }

                val requesterDoc = firestore.collection("users").document(event.hostId).get().await()
                val requesterProfileImage = requesterDoc.getString("profileImageUrl") ?: ""

                val participantId = "host_plusOne_${UUID.randomUUID()}"
                val participant = ParticipantInfo(
                    userId = participantId,
                    userName = "$friendName (+1 of Host)",
                    profileImageUrl = requesterProfileImage,
                    age = 0,
                    gender = ""
                )

                eventRef.collection("participants").document(participantId).set(participant).await()
                loadParticipants(event.id)

                onResult(true, "+1 added successfully.")
            } catch (e: Exception) {
                Log.e("AddPlusOneError", "Error adding +1", e)
                onResult(false, e.message ?: "Failed to add +1.")

            }
        }
    }

    fun kickParticipant(event: Event, userId: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                firestore.collection("events")
                    .document(event.id)
                    .collection("participants")
                    .document(userId)
                    .delete()
                    .await()


                //  Remove from chat only if not host
                if (userId != event.hostId) {
                    removeMemberFromChat(event.id, userId)
                }

                //  Send notification to kicked user
                sendNotification(
                    toUserId = userId,
                    title = "Removed from Event",
                    message = "You have been removed from '${event.title}'.",
                    type = "kicked",
                    eventId = event.id
                )

                onComplete(true, "Participant removed.")
                loadParticipants(event.id) // Refresh list
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    suspend fun getUserName(userId: String): String {
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            val userName = snapshot.getString("name")
            userName ?: "Anonymous"
        } catch (e: Exception) {
            "Anonymous"
        }
    }

    // Chat Feature Adding User
    fun addMemberToChat(
        eventId: String,
        userId: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "eventId" to eventId,
                    "userId" to userId
                )

                Firebase.functions
                    .getHttpsCallable("addUserToEventChat")
                    .call(data)
                    .await()

                Log.d("Chat", "User added to chat via Cloud Function.")
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("Chat", "Failed to add user to chat: ${e.message}")
                onResult(false, e.message)
            }
        }
    }


    open fun removeMemberFromChat(eventId: String, userId: String) {
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "eventId" to eventId,
                    "userId" to userId
                )

                Firebase.functions
                    .getHttpsCallable("removeUserFromEventChat")
                    .call(data)
                    .await()

                Log.d("Chat", "User removed from chat via Cloud Function.")
            } catch (e: Exception) {
                Log.e("Chat", "Failed to remove user from chat: ${e.message}")
            }
        }
    }

}


