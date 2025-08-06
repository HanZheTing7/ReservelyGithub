package com.example.reservely.presentation.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.reservely.presentation.features.chat.ChannelListScreen
import com.example.reservely.presentation.features.chat.ChatHelper
import com.example.reservely.presentation.features.chat.ChatHelperViewModel
import com.example.reservely.presentation.features.create_event.CreateEventScreen
import com.example.reservely.presentation.features.explore.FilterBottomSheet
import com.example.reservely.presentation.features.joined_event.JoinedEventsScreen
import com.example.reservely.presentation.features.explore.ReservelyAppBar
import com.example.reservely.presentation.features.explore.HomeViewModel
import com.example.reservely.presentation.features.explore.ExploreScreen
import com.example.reservely.presentation.features.notification.NotificationsViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(
    navController: NavHostController,
    notificationsViewModel: NotificationsViewModel = viewModel(),
    placesClient: PlacesClient,
    chatHelper: ChatHelper = hiltViewModel<ChatHelperViewModel>().chatHelper // Get ChatHelper via ViewModel
) {
    val context = LocalContext.current
    val bottomNavController = rememberNavController()
    val currentDestination = bottomNavController.currentBackStackEntryAsState().value?.destination?.route

    val isChatMessageScreen = currentDestination?.startsWith("messages") == true
    val isChatTab = currentDestination == "chat" || isChatMessageScreen

    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedCategories by rememberSaveable { mutableStateOf(listOf<String>()) }
    var selectedPlaces by rememberSaveable { mutableStateOf(listOf<String>()) }
    var nearMe by rememberSaveable { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationSaver = Saver<Location?, Any>(
        save = { it?.let { listOf(it.latitude, it.longitude) } },
        restore = {
            if (it is List<*> && it.size == 2) {
                val lat = it[0] as? Double
                val lng = it[1] as? Double
                if (lat != null && lng != null) Location("").apply {
                    latitude = lat
                    longitude = lng
                } else null
            } else null
        }
    )

    var userLocation by rememberSaveable(stateSaver = locationSaver) { mutableStateOf<Location?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) userLocation = location
            }
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            nearMe = false
        }
    }

    LaunchedEffect(nearMe) {
        if (nearMe) {
            val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) userLocation = location
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            chatHelper.connectUser(user.uid) { success, error ->
                if (success) {
                    Log.d("Chat", "Connected to Stream")
                } else {
                    Log.e("Chat", "Stream connection failed: $error")
                }
            }
        }
    }




    Scaffold(
        topBar = {
            ReservelyAppBar(
                viewModel = notificationsViewModel,
                onProfileClick = { navController.navigate("profile") },
                onFilterClick = { showFilterSheet = true },
                onNotificationClick = { navController.navigate("notification") },
                showFilterIcon = currentDestination == "explore"
            )
        },
        bottomBar = {
            NavigationBar {
                val navItems = listOf(
                    NavItem("MyEvents", Icons.Default.Event),
                    NavItem("Explore", Icons.Default.Search),
                    NavItem("Create", Icons.Default.Add),
                    NavItem("Chat", Icons.AutoMirrored.Filled.Chat)
                )

                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = when (item.label.lowercase()) {
                            "chat" -> isChatTab
                            else -> currentDestination == item.label.lowercase()
                        },
                        onClick = {
                            bottomNavController.navigate(item.label.lowercase()) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->

        if (showFilterSheet && currentDestination == "explore") {
            FilterBottomSheet(
                selectedCategories = selectedCategories,
                onCategoryChange = { selectedCategories = it },
                selectedPlaces = selectedPlaces,
                onPlaceChange = { selectedPlaces = it },
                nearMe = nearMe,
                onNearMeToggle = { nearMe = it },
                onDismiss = { showFilterSheet = false }
            )
        }

        NavHost(
            navController = bottomNavController,
            startDestination = "myevents",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Properly apply Scaffold padding here
        ) {
            composable("myevents") {
                JoinedEventsScreen(navController = navController)
            }

            composable("explore") {
                val homeViewModel: HomeViewModel = hiltViewModel()
                ExploreScreen(
                    viewModel = homeViewModel,
                    navController = navController,
                    selectedCategories = selectedCategories,
                    selectedPlaces = selectedPlaces,
                    nearMe = nearMe,
                    userLocation = userLocation
                )
            }

            composable("create") {
                CreateEventScreen(
                    placesClient = placesClient,
                    onEventCreated = { createdEvent ->
                        chatHelper.createEventChannel(
                            eventId = createdEvent.id,
                            eventName = createdEvent.title,
                            hostId = FirebaseAuth.getInstance().currentUser!!.uid,
                            participantIds = emptyList()
                        ) { success, error ->
                            if (success) Log.d("CreateEvent", "Channel created for event ${createdEvent.id}")
                            else Log.e("CreateEvent", "Failed to create channel: $error")
                        }
                        bottomNavController.popBackStack()
                    }
                )
            }

            composable("chat") {
                ChannelListScreen(navController = navController, bottomNavController = bottomNavController)
            }

        }
    }
}





