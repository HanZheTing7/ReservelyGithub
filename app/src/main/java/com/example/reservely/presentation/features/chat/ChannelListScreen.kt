package com.example.reservely.presentation.features.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChannelListScreen(navController: NavController, bottomNavController: NavController) {
    val viewModel: EventChannelListViewModel = hiltViewModel()
    val channels by viewModel.channels.collectAsState()

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    EventChannelList(
        navController = navController,
        channels = channels,
        currentUserId = currentUserId, // <-- Pass current user id here
        isRefreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.queryEventChannels() }
    )
}
