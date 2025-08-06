package com.example.reservely.presentation.features.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.reservely.data.remote.CloudinaryApi
import com.example.reservely.di.CloudinaryConfig
import com.example.reservely.di.IoDispatcher
import com.example.reservely.data.model.UserProfile
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
open class ProfileViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val cloudinaryApi: CloudinaryApi,
    private val config: CloudinaryConfig,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _userData = mutableStateOf<UserProfile?>(null)
    val userData: State<UserProfile?> = _userData

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl: StateFlow<String?> = _uploadedImageUrl

    suspend fun uploadProfileImageToCloudinary(
        context: Context,
        uri: Uri,
    ): String? {
        val file = uriToFile(context, uri)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val preset = config.uploadPreset.toRequestBody("text/plain".toMediaTypeOrNull())

        val response = cloudinaryApi.uploadImage(body, preset)

        return if (response.isSuccessful) response.body()?.secure_url else null
    }


    private fun uriToFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File.createTempFile("upload", ".jpg", context.cacheDir)
        inputStream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        return file
    }

    init {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("ProfileViewModel", "User not logged in — currentUser is null")
        } else {
            Log.d("ProfileViewModel", "User ID: $uid")
        }

        loadUserProfile()
    }

    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        val isGoogleSignIn = auth.currentUser?.providerData?.any {
            it.providerId == "google.com"
        } == true

        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    if (profile != null) {
                        // Ensure Google Sign-In flag is updated correctly
                        _userData.value = profile.copy(
                            email = auth.currentUser?.email ?: profile.email,
                            isGoogleSignIn = isGoogleSignIn
                        )
                    } else {
                        Log.e("ProfileViewModel", "Failed to parse user profile")
                    }
                } else {
                    // No Firestore profile, use FirebaseAuth data
                    val firebaseUser = auth.currentUser
                    val defaultProfile = UserProfile(
                        name = firebaseUser?.displayName ?: "Unknown",
                        gender = "",
                        age = 0,
                        profileImageUrl = firebaseUser?.photoUrl?.toString() ?: "",
                        email = firebaseUser?.email ?: "",
                        isGoogleSignIn = isGoogleSignIn
                    )

                    firestore.collection("users").document(uid)
                        .set(defaultProfile)
                        .addOnSuccessListener {
                            Log.d("ProfileViewModel", "Default profile saved to Firestore")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProfileViewModel", "Failed to save default profile", e)
                        }

                    _userData.value = defaultProfile
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileViewModel", "Failed to load user profile", e)
            }
    }


    fun updateUserProfile(updated: UserProfile, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onError("User not signed in")
        firestore.collection("users").document(uid)
            .set(updated)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Error") }

    }

    fun reauthenticateAndUpdateCredentials(
        email: String,
        password: String,
        newPassword: String?,
        newEmail: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser ?: return onResult(false, "User not logged in")
        val credential = EmailAuthProvider.getCredential(email, password)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                val tasks = mutableListOf<Task<Void>>()

                if (!newPassword.isNullOrBlank()) {
                    tasks.add(user.updatePassword(newPassword))
                }

                if (!newEmail.isNullOrBlank() && newEmail != user.email) {
                    tasks.add(user.updateEmail(newEmail))
                }

                if (tasks.isEmpty()) {
                    onResult(true, null)
                    return@addOnSuccessListener
                }

                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    if (it.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, it.exception?.message ?: "Failed to update credentials")
                    }
                }
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return onError("No user signed in")
        val uid = user.uid

        // Step 1: Delete Firestore user document
        firestore.collection("users").document(uid).delete()
            .addOnSuccessListener {
                // Step 2: Delete Firebase Auth account
                user.delete()
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener {
                        onError("Failed to delete auth account: ${it.message}")
                    }
            }
            .addOnFailureListener {
                onError("Failed to delete Firestore data: ${it.message}")
            }
    }

}

