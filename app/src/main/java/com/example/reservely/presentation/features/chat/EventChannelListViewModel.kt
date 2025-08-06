package com.example.reservely.presentation.features.chat

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.chat.android.models.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class EventChannelListViewModel @Inject constructor(
    private val chatHelper: ChatHelper,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels

    var isRefreshing by mutableStateOf(false)
        private set

    init {
        queryEventChannels()
    }

    fun queryEventChannels() {
        isRefreshing = true

        val currentUserId = auth.currentUser?.uid ?: return

        chatHelper.queryUserChannels(currentUserId) { result, error ->
            isRefreshing = false

            if (result != null) {
                _channels.value = result
            } else {
                Log.e("Chat", "Channel query failed: $error")
            }
        }
    }
}
