package com.example.reservely.presentation.features.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class EmailVerificationUiState(
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val error: String? = null,
    val emailResent: Boolean = false
)

@Composable
fun EmailVerificationScreen(
    email: String,
    password: String,
    name: String,
    gender: String,
    age: Int,
    navController: NavController,
    viewModel: LoginScreenViewModel = hiltViewModel()
) {
    val state by viewModel.emailVerificationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val timer = remember { mutableIntStateOf(60) }
    val isClickable = remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.startEmailVerificationPolling(name, gender, age, email)

        onDispose {
            viewModel.stopEmailVerificationPolling()
        }
    }

    LaunchedEffect(state.isVerified) {
        if (state.isVerified) {
            navController.navigate("onboarding") {
                popUpTo("register") { inclusive = true
                }
            }
        }
    }

    LaunchedEffect(state.emailResent) {
        if (state.emailResent) {
            scope.launch {
                snackbarHostState.showSnackbar("Verification email resent.")
            }
        }
    }

    // Countdown timer - will restart when timer is reset
    LaunchedEffect(isClickable.value) {
        if (!isClickable.value) {
            timer.intValue = 60
            while (timer.intValue > 0) {
                delay(1000)
                timer.intValue -= 1
            }
            isClickable.value = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Verify Your Email", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "A verification link has been sent to $email.",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(80.dp))

            if (isClickable.value) {
                Text(
                    text = "Resend Email",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
                        isClickable.value = false // Will trigger LaunchedEffect

                        // Show snackbar
                        scope.launch {
                            snackbarHostState.showSnackbar("Verification email resent.")
                        }
                    }
                )
            } else {
                Text(
                    text = "Resend Email (${timer.intValue}s)",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}