package com.example.reservely.presentation.features.edit_event

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.places.api.net.PlacesClient

@Composable
fun EditEventScreen(eventId: String, onEventUpdated: () -> Unit,  placesClient: PlacesClient, viewModel: EditEventViewModel = hiltViewModel()) {
    val context = LocalContext.current

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    if (viewModel.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        viewModel.event?.let { event ->
            EditEventForm(
                event = event,
                placesClient = placesClient,
                onEventUpdated = onEventUpdated
            )
        } ?: run {
            Toast.makeText(context, "Failed to load event", Toast.LENGTH_SHORT).show()
        }
    }
}

