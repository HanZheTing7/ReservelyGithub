package com.example.reservely.presentation.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.reservely.presentation.features.chat.MessageScreen
import com.example.reservely.data.model.Event
import com.example.reservely.data.model.RegistrationInfo
import com.example.reservely.presentation.features.notification.NotificationScreen
import com.example.reservely.presentation.features.edit_event.EditEventScreen
import com.example.reservely.presentation.features.auth.EmailVerificationScreen
import com.example.reservely.presentation.features.event_detail.EventDetailScreen
import com.example.reservely.presentation.features.auth.LoginScreen
import com.example.reservely.presentation.features.onboarding.OnboardingScreen
import com.example.reservely.presentation.features.profile.ProfileScreen
import com.example.reservely.presentation.features.auth.RegisterScreen
import com.example.reservely.presentation.features.event_detail.EventDetailViewModel
import com.example.reservely.presentation.features.notification.NotificationsViewModel
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AppNavigation(placesClient: PlacesClient) {
    // Creates a controller to manage navigation state
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    val notificationsViewModel: NotificationsViewModel = hiltViewModel()

    var isLoading by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser

        startDestination = when {
            user == null -> "login"

            user.isEmailVerified.not() -> "emailVerification"

            else -> {
                val doc = firestore.collection("users").document(user.uid).get().await()
                val hasCompleted = doc.getBoolean("hasCompletedOnboarding") ?: false
                if (hasCompleted) "main" else "onboarding"
            }
        }

        isLoading = false
    }


    startDestination?.let { start ->
        NavHost(navController = navController, startDestination = start) {
            composable("onboarding") {
                OnboardingScreen {
                    // Mark onboarding completed in Firestore
                    firebaseUser?.let { user ->
                        firestore.collection("users").document(user.uid)
                            .update("hasCompletedOnboarding", true)
                    }
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            }

            composable("login") {
                LoginScreen(
                    navController = navController,
                    onNavigateToHome = {
                        // This logic now runs right after a successful login
                        scope.launch {
                            val user = FirebaseAuth.getInstance().currentUser!!
                            val doc = firestore.collection("users").document(user.uid).get().await()
                            val hasCompleted = doc.getBoolean("hasCompletedOnboarding") ?: false
                            val destination = if (hasCompleted) "main" else "onboarding"

                            navController.navigate(destination) {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    navController = navController,
                    onNavigateToHome = {
                        // Also apply the same logic here for new users
                        scope.launch {
                            val user = FirebaseAuth.getInstance().currentUser!!
                            // For new users, you can assume onboarding is not complete
                            // or perform the check for robustness.
                            val doc = firestore.collection("users").document(user.uid).get().await()
                            val hasCompleted = doc.getBoolean("hasCompletedOnboarding") ?: false
                            val destination = if (hasCompleted) "main" else "onboarding"

                            navController.navigate(destination) {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                )
            }

            composable("emailVerification") {
                val registrationInfo =
                    navController.previousBackStackEntry?.savedStateHandle?.get<RegistrationInfo>("registrationInfo")

                if (registrationInfo != null) {
                    EmailVerificationScreen(
                        email = registrationInfo.email,
                        password = registrationInfo.password,
                        name = registrationInfo.name,
                        gender = registrationInfo.gender,
                        age = registrationInfo.age,
                        navController = navController
                    )
                }
            }


            // Defines the "explore" screen destination
            composable("main") {
                MainScreen(
                    navController = navController,
                    notificationsViewModel = notificationsViewModel,
                    placesClient = placesClient
                )
            }

            composable(
                route = "eventDetail/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId")
                if (eventId != null) {
                    EventDetailScreenLoader(eventId = eventId, navController = navController)
                }
            }

            composable(
                route = "editEvent/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId")
                if (eventId != null) {
                    EditEventScreen(
                        placesClient = placesClient,
                        eventId = eventId,
                        onEventUpdated = {
                            navController.popBackStack()
                        })
                }
            }


            composable("profile") {
                ProfileScreen(navController = navController)
            }

            composable("notification") {
                NotificationScreen(
                    navController = navController,
                    notificationsViewModel = notificationsViewModel
                )
            }

            composable(
                route = "messages?cid={cid}",
                arguments = listOf(navArgument("cid") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedCid = backStackEntry.arguments?.getString("cid") ?: return@composable
                val channelId = Uri.decode(encodedCid)

                MessageScreen(navController = navController, channelId = channelId)
            }

        }
    }
}

@Composable
fun EventDetailScreenLoader(eventId: String, navController: NavController) {
    val context = LocalContext.current
    val viewModel: EventDetailViewModel = hiltViewModel()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var event by remember { mutableStateOf<Event?>(null) }
    LaunchedEffect(eventId) {
        FirebaseFirestore.getInstance()
            .collection("events")
            .document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                doc.toObject(Event::class.java)?.let {
                    event = it.copy(id = eventId)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load event", Toast.LENGTH_SHORT).show()
            }
    }

    if (event == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        EventDetailScreen(
            navController = navController,
            event = event!!,
            currentUserId = currentUserId,
            onEditClick = {
                navController.navigate("editEvent/${event?.id}")
            },
            viewModel = viewModel
        )
    }
}


