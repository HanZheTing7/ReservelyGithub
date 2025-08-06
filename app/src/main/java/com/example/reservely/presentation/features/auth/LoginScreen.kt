package com.example.reservely.presentation.features.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.reservely.R
import com.example.reservely.presentation.components.CustomOutlinedTextField
import com.example.reservely.presentation.components.requestNotificationPermissionIfNeeded
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.identity.Identity

@Composable
fun LoginScreen(viewModel: LoginScreenViewModel = hiltViewModel(), navController: NavController, onNavigateToHome:() -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    //Forgot Password state
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by rememberSaveable { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }


    var permissionRequested by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            // Request permission if not done already
            if (!permissionRequested) {
                permissionRequested = true
                requestNotificationPermissionIfNeeded(context)
            }

            onNavigateToHome()
            viewModel.resetState()
        }
    }
// Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val oneTapClient = Identity.getSignInClient(context)
                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    val googleIdToken = credential.googleIdToken
                    if (googleIdToken != null) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        viewModel.signInWithGoogle(firebaseCredential)
                    }
                } catch (e: ApiException) {
                    Log.e("LoginScreen", "Google Sign-In failed", e)
                    viewModel.showError("Google Sign-In failed.")
                }
            }
        }
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Reservely", style = MaterialTheme.typography.headlineMedium,
            fontStyle = FontStyle.Italic,
        )
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.therainz), // Replace with your app logo
            contentDescription = "App Logo",
            modifier = Modifier
                .size(240.dp),
            contentScale = ContentScale.Fit
        )

        // Email Text Field
        CustomOutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.resetError() },
            label = "Email",
            keyboardOptions = KeyboardType.Email,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Text Field
        CustomOutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.resetError() },
            label = "Password",
            singleLine = true,
            keyboardOptions = KeyboardType.Password,
            isPassword = true
        )

        // Display the error message from the ViewModel
        uiState.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Login Button
        Button(
            onClick = {
                viewModel.signInWithEmailAndPassword(email, password)
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
        ) {
            Text("Continue")
        }

        Spacer(modifier = Modifier.height(32.dp))

        DividerWithOrText()

        Spacer(modifier = Modifier.height(32.dp))

        // Google Sign-In Button
        OutlinedButton(
            onClick = {
                val oneTapClient = Identity.getSignInClient(context)
                val signInRequest = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId(context.getString(R.string.your_web_client_id))
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .build()

                oneTapClient.beginSignIn(signInRequest)
                    .addOnSuccessListener { result ->
                        val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                        googleSignInLauncher.launch(intentSenderRequest)
                    }
                    .addOnFailureListener { e ->
                        Log.e("LoginScreen", "Google Sign-In failed", e)
                        viewModel.showError("No Google account found. Please add one in your device settings.")
                    }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground), // Add your Google logo to drawables
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign in with Google")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = { navController.navigate("register") }) {
            Text("Don't have an account? Create one")
        }

        TextButton(onClick = {
            resetEmail = email // Pre-fill with whatever user typed
            showResetDialog = true
        }) {
            Text("Forgot Password?")
        }


    }
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Enter your email") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email)
                    )
                    resetMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.sendPasswordResetEmail(resetEmail) { success, message ->
                            if (success) {
                                resetMessage = "Reset email sent. Check your inbox."
                            } else {
                                resetMessage = message
                            }
                        }
                    }
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    resetMessage = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

}

@Composable
fun DividerWithOrText() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(horizontal = 16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier
                .weight(1f),
            thickness = 2.dp,
            color = Color.Gray
        )

        Text(
            text = "or",
            modifier = Modifier.padding(horizontal = 8.dp),
            color = Color.Gray
        )

        HorizontalDivider(
            modifier = Modifier
                .weight(1f),
            thickness = 2.dp,
            color = Color.Gray
        )
    }
}


//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    ReservelyTheme {
//        LoginScreen()
//    }
//}