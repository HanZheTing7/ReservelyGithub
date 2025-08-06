package com.example.reservely.presentation.features.explore

import android.app.Activity
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ExploreScreen(viewModel: HomeViewModel,
                  navController: NavController,
                  selectedCategories: List<String> = emptyList(),
                  selectedPlaces: List<String> = emptyList(),
                  nearMe: Boolean = false,
                  userLocation: Location? = null) {

    val today = viewModel.today.value
    val selectedDate = viewModel.selectedDate.value

    if (today == null || selectedDate == null) {
        val fallback = LocalDate.now(ZoneId.of("Asia/Kuala_Lumpur"))
        LaunchedEffect(Unit) {
            viewModel.onDateSelected(fallback)
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }


    val uiState = CalendarUiState(today = today, selectedDate = selectedDate)
    val context = LocalContext.current
    var backPressedTime by remember { mutableStateOf(0L) }

    // Handle double back press to exit
    BackHandler {
        val now = System.currentTimeMillis()
        if (now - backPressedTime < 2000) {
            (context as? Activity)?.finish() //  Exits the app
        } else {
            backPressedTime = now
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    HomeScreen(
        viewModel = viewModel,
        uiState = uiState,
        onDateSelected = viewModel::onDateSelected,
        navController = navController,
        selectedCategories = selectedCategories,
        selectedPlaces = selectedPlaces,
        nearMe = nearMe,
        userLocation = userLocation
    )
}