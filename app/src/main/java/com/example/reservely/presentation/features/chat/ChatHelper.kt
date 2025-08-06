package com.example.reservely.presentation.features.chat

import android.util.Log
import com.example.reservely.data.model.UserProfile
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.api.models.QueryChannelsRequest
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatHelper @Inject constructor(
    private val chatClient: ChatClient,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {

    private val TAG = "ChatHelper"
    private val pendingActions = mutableListOf<() -> Unit>()

    fun connectUser(
        userId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val currentUser = chatClient.getCurrentUser()

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val userProfile = doc.toObject(UserProfile::class.java)

                val finalName = userProfile?.name?.takeIf { it.isNotBlank() } ?: "Unknown"
                val finalImage = userProfile?.profileImageUrl ?: ""

                val user = User(
                    id = userId,
                    name = finalName,
                    image = finalImage
                )

                val data = mapOf(
                    "userId" to userId,
                    "name" to finalName,
                    "image" to finalImage
                )

                functions.getHttpsCallable("syncStreamUserProfile")
                    .call(data)
                    .addOnSuccessListener {
                        Log.d(TAG, "Synced profile. Proceeding to connectUser.")

                        when {
                            currentUser == null -> performConnect(user, onResult)
                            currentUser.id == userId -> {
                                Log.d(TAG, "Already connected as $userId.")
                                runPendingActions()
                                onResult(true, null)
                            }
                            else -> {
                                chatClient.disconnect(flushPersistence = true).enqueue { res ->
                                    if (res.isSuccess) {
                                        performConnect(user, onResult)
                                    } else {
                                        val error = res.errorOrNull()?.message ?: "Disconnect error"
                                        onResult(false, error)
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        onResult(false, e.message)
                    }
            }
            .addOnFailureListener { e ->
                onResult(false, e.message)
            }
    }

    private fun performConnect(user: User, onResult: (Boolean, String?) -> Unit) {
        functions.getHttpsCallable("generateStreamToken")
            .call()
            .addOnSuccessListener { result ->
                val token = (result.data as? Map<*, *>)?.get("token") as? String

                if (token != null) {
                    chatClient.connectUser(user, token).enqueue { res ->
                        if (res.isSuccess) {
                            runPendingActions()
                            onResult(true, null)
                        } else {
                            onResult(false, res.errorOrNull()?.message ?: "Unknown error")
                        }
                    }
                } else {
                    onResult(false, "Token is null")
                }
            }
            .addOnFailureListener { e ->
                onResult(false, e.message)
            }
    }

    fun createDirectMessageChannel(
        currentUserId: String,
        targetUserId: String,
        onResult: (Channel?, String?) -> Unit
    ) {
        val syncTasks = listOf(currentUserId, targetUserId).map { userId ->
            functions.getHttpsCallable("syncStreamUserProfile").call(mapOf("userId" to userId))
        }

        Tasks.whenAllComplete(syncTasks)
            .addOnSuccessListener {
                val members = listOf(currentUserId, targetUserId).sorted()
                val channelId = "dm_${members.joinToString("_")}"

                val extraData = mapOf("is_direct_message" to true)

                chatClient.createChannel(
                    channelType = "messaging",
                    channelId = channelId,
                    memberIds = members,
                    extraData = extraData
                ).enqueue { result ->
                    if (result.isSuccess) {
                        onResult(result.getOrNull(), null)
                    } else {
                        val error = result.errorOrNull()
                        if (error?.message?.contains("already exists") == true) {
                            chatClient.channel("messaging", channelId).watch().enqueue { watchResult ->
                                if (watchResult.isSuccess) {
                                    onResult(watchResult.getOrNull(), null)
                                } else {
                                    onResult(null, watchResult.errorOrNull()?.message)
                                }
                            }
                        } else {
                            onResult(null, error?.message)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                onResult(null, e.message)
            }
    }

    fun createEventChannel(
        eventId: String,
        eventName: String,
        hostId: String,
        participantIds: List<String>,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val channelType = "messaging"
        val channelId = "event_$eventId"
        val channelClient = chatClient.channel(channelType, channelId)
        val allMembers = (participantIds + hostId).distinct()

        val extraData = mapOf(
            "name" to eventName,
            "is_event" to true,
            "host_id" to hostId
        )

        val action = {
            Log.d(TAG, "Creating event channel: $channelId with members: $allMembers")

            chatClient.createChannel(
                channelType = channelType,
                channelId = channelId,
                memberIds = allMembers,
                extraData = extraData
            ).enqueue { result ->
                if (result.isSuccess) {
                    onResult(true, null)
                } else {
                    val error = result.errorOrNull()
                    if (error?.message?.contains("already exists") == true) {
                        channelClient.update(extraData = extraData, message = null).enqueue { updateResult ->
                            if (updateResult.isSuccess) {
                                onResult(true, null)
                            } else {
                                onResult(false, updateResult.errorOrNull()?.message ?: "Unknown error")
                            }
                        }
                    } else {
                        onResult(false, error?.message ?: "Unknown error")
                    }
                }
            }
        }

        if (chatClient.getCurrentUser() != null) {
            action()
        } else {
            pendingActions.add(action)
        }
    }

    fun queryUserChannels(
        currentUserId: String,
        onResult: (List<Channel>?, String?) -> Unit
    ) {
        val filter = Filters.and(
            Filters.eq("type", "messaging"),
            Filters.`in`("members", listOf(currentUserId))
        )

        val request = QueryChannelsRequest(filter, limit = 50).apply {
            messageLimit = 1
            memberLimit = 10
        }.withWatch().withState()

        chatClient.queryChannels(request).enqueue { result ->
            if (result.isSuccess) {
                onResult(result.getOrNull(), null)
            } else {
                onResult(null, result.errorOrNull()?.message ?: "Unknown error")
            }
        }
    }


    fun disconnectUser() {
        chatClient.disconnect(flushPersistence = true)
        pendingActions.clear()
    }

    private fun runPendingActions() {
        pendingActions.forEach { it() }
        pendingActions.clear()
    }
}
