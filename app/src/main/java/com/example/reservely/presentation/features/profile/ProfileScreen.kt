package com.example.reservely.presentation.features.profile

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavController
import com.example.reservely.presentation.features.chat.ChatHelper
import com.example.reservely.presentation.features.chat.ChatHelperViewModel
import com.example.reservely.presentation.components.CloudImagePicker
import com.example.reservely.presentation.components.GenderSelector
import kotlinx.coroutines.launch


@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    navController: NavController,
    chatHelper: ChatHelper = hiltViewModel<ChatHelperViewModel>().chatHelper
) {
    val context = LocalContext.current
    val profile = viewModel.userData.value ?: return

    // Editable states
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var age by rememberSaveable { mutableStateOf(profile.age?.toString() ?: "") }
    var password by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(profile.email) }
    var selectedCategory by rememberSaveable { mutableStateOf(profile.gender) }
    var tempPassword by rememberSaveable { mutableStateOf("") }


    // Dialog states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordStrength by remember { mutableStateOf("Weak") }
    var reauthAction: (() -> Unit)? by remember { mutableStateOf(null) }

    val uploading by viewModel.uploading.collectAsState()
    val uploadedUrl by viewModel.uploadedImageUrl.collectAsState()

    // This state holds the currently displayed profile image URL
    var profileImage by remember { mutableStateOf(profile.profileImageUrl) }
    var imageUri by remember { mutableStateOf<Uri?>(null) } // New local selected image
    var profileImageUrl by remember { mutableStateOf(profile.profileImageUrl) } // Current or uploaded image
    var imageChanged by rememberSaveable { mutableStateOf(false) }

    var showAdditionalSettings by rememberSaveable { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Save Changes Button disable state if no edit is made
    val hasChanges by remember(
        name,
        age,
        email,
        password,
        selectedCategory,
        imageChanged
    ) {
        derivedStateOf {
            name != profile.name ||
                    age != profile.age.toString() ||
                    email != profile.email ||
                    password.isNotBlank() ||
                    selectedCategory != profile.gender ||
                    imageChanged
        }
    }


    // Update local image when upload completes
    LaunchedEffect(uploadedUrl) {
        uploadedUrl?.let { profileImage = it }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text("Edit Profile", style = MaterialTheme.typography.titleLarge)

            // Profile Image
            CloudImagePicker(
                imageUrl = imageUri?.toString() ?: profileImageUrl,
                uploading = false, // Not uploading yet
                onUploadRequested = { uri ->
                    imageUri = uri
                    imageChanged = true
                }
            )

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })


            GenderSelector(
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = it
                }
            )


            OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") })

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                enabled = !profile.isGoogleSignIn,
            )

            if (!profile.isGoogleSignIn) {
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = if (it.isNotBlank() && it.length < 6) {
                            "Password must be at least 6 characters"
                        } else null
                        passwordStrength = when {
                            it.length < 6 -> "Weak"
                            it.matches(Regex("^[a-zA-Z]*$")) -> "Medium"
                            else -> "Strong"
                        }
                    },
                    label = { Text("New Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        val description = if (passwordVisible) "Hide password" else "Show password"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, description)
                        }
                    },
                    supportingText = {
                        Column {
                            passwordError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (password.isNotBlank()) {
                                val strengthColor = when (passwordStrength) {
                                    "Weak" -> Color.Red
                                    "Medium" -> Color(0xFFFFA000) // amber
                                    "Strong" -> Color(0xFF388E3C) // green
                                    else -> Color.Gray
                                }

                                Text(
                                    text = "Strength: $passwordStrength",
                                    color = strengthColor,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                )
            } else {
                Text(
                    text = "Signed in via Google. Password and email cannot be changed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Button(
                onClick = {
                    // Update password/email with reauth
                    if (!profile.isGoogleSignIn && (password.isNotBlank() || email != profile.email)) {
                        if (password.isNotBlank() && password.length < 6) {
                            Toast.makeText(
                                context,
                                "Password must be at least 6 characters",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            val finalImageUrl = if (imageChanged && imageUri != null) {
                                viewModel.uploadProfileImageToCloudinary(
                                    context = context,
                                    uri = imageUri!!
                                )
                            } else {
                                profileImageUrl // Use existing
                            }

                            val updated = profile.copy(
                                name = name,
                                gender = selectedCategory,
                                age = age.toIntOrNull() ?: 0,
                                profileImageUrl = finalImageUrl ?: "",
                                email = email
                            )

                            if (!profile.isGoogleSignIn && (password.isNotBlank() || email != profile.email)) {
                                showReauthDialog = true
                                reauthAction = {
                                    viewModel.reauthenticateAndUpdateCredentials(
                                        email = profile.email,
                                        password = tempPassword,
                                        newPassword = password.ifBlank { null },
                                        newEmail = if (email != profile.email) email else null
                                    ) { success, error ->
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                "Credentials updated",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Update failed: $error",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            } else {
                                viewModel.updateUserProfile(updated,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Profile Updated",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                        imageChanged = false
                                    },
                                    onError = {
                                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        } finally {
                            // This will run whether the try block succeeds or fails
                            isLoading = false
                        }
                    }
                },
                enabled = hasChanges,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasChanges) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) {
                if (isLoading) {
                    // Show a loading spinner
                    CircularProgressIndicator(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary // Or any color you prefer
                    )
                } else {
                    // Show the normal text
                    Text("Save Changes")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            val rotationAngle by animateFloatAsState(
                targetValue = if (showAdditionalSettings) 180f else 0f,
                label = "rotation"
            )

// A Divider adds a nice visual separation from the content above
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(WindowInsets.navigationBars.asPaddingValues())
            ) {
                // This Row is the clickable header for the expandable section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdditionalSettings = !showAdditionalSettings }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // This pushes the text and icon apart
                ) {
                    Text(
                        text = "Additional Settings",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Additional Settings",
                        modifier = Modifier.rotate(rotationAngle) // Apply the animated rotation
                    )
                }

                // This will animate the appearance of the buttons with a slide & fade effect
                AnimatedVisibility(
                    visible = showAdditionalSettings,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // The Delete Account Button now has a subtle background to indicate danger
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Account", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }
        }
        IconButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .align(Alignment.TopStart) // This positions it at the top left
                .padding(8.dp)
                .padding(WindowInsets.statusBars.asPaddingValues())
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Logout"
            )
        }
    }

    if (showReauthDialog) {
        AlertDialog(
            onDismissRequest = {
                showReauthDialog = false
                tempPassword = ""
            },
            title = { Text("Re-authenticate") },
            text = {
                Column {
                    Text("Please enter your current password to confirm changes.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReauthDialog = false
                        reauthAction?.invoke()
                        tempPassword = ""
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReauthDialog = false
                        tempPassword = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        chatHelper.disconnectUser()
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount(
                            onSuccess = {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

