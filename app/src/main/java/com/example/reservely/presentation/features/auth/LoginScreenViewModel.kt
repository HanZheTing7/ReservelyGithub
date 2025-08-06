package com.example.reservely.presentation.features.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reservely.data.model.UserProfile
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val verificationSent: Boolean = false
)

@HiltViewModel
class LoginScreenViewModel @Inject constructor(private val firestore: FirebaseFirestore, val auth: FirebaseAuth):ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _emailVerificationState = MutableStateFlow(EmailVerificationUiState())
    val emailVerificationState: StateFlow<EmailVerificationUiState> = _emailVerificationState

    private var pollingJob: Job? = null

    fun signInWithEmailAndPassword(email: String, password:String) = viewModelScope.launch{
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState(error = "Please fill in your email and password.")
            return@launch
        }

        _uiState.value = LoginUiState(isLoading = true)
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user

            user?.reload()?.await() //  Force reload to get latest verification state

            if (user != null && user.isEmailVerified) {
                saveFcmTokenToFirestore(user.uid)
                _uiState.value = LoginUiState(isSuccess = true)
            } else {
                auth.signOut()
                _uiState.value = LoginUiState(error = "Please verify your email before logging in.")
            }
        }catch (ex:Exception){
            val errorMessage = when (ex) {
                is FirebaseAuthInvalidUserException,
                is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                else -> ex.message ?: "Login failed. Please try again."
            }
            _uiState.value = LoginUiState(error = errorMessage)
        }
    }

    fun signInWithGoogle(credential: AuthCredential) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            try {
                auth.signInWithCredential(credential).await()
                auth.currentUser?.uid?.let { saveFcmTokenToFirestore(it) }
                _uiState.value = LoginUiState(isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = LoginUiState(error = e.message)
            }
        }
    }

    fun registerWithEmail(email: String, password: String, name: String, gender: String, age: Int) = viewModelScope.launch {
        _uiState.value = LoginUiState(isLoading = true)

        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User is null")

            user.sendEmailVerification().await()

            val userData = mapOf(
                "name" to name,
                "gender" to gender,
                "age" to age,
                "email" to email,
                "isEmailVerified" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(user.uid).set(userData).await()
            _uiState.value = LoginUiState(verificationSent = true)

        } catch (e: Exception) {
            if ((e as? FirebaseAuthUserCollisionException) != null) {
                // Account exists, try logging in
                try {
                    val loginResult = auth.signInWithEmailAndPassword(email, password).await()
                    val user = loginResult.user

                    if (user != null && !user.isEmailVerified) {
                        user.sendEmailVerification().await()

                        _uiState.value = LoginUiState(verificationSent = true)
                        return@launch
                    } else {
                        _uiState.value = LoginUiState(error = "Account already exists and is verified. Please sign in.")
                    }

                } catch (signInException: Exception) {
                    _uiState.value = LoginUiState(error = "Account already exists. Please use correct password to continue verification.")
                }
            } else {
                _uiState.value = LoginUiState(error = e.message ?: "Registration failed")
            }
        }
    }

    fun saveVerifiedUserToFirestore(name: String, gender: String, age: Int, email: String) = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch

        val userProfile = UserProfile(
            name = name,
            gender = gender,
            age = age,
            profileImageUrl = "",
            email = email,
            isGoogleSignIn = false,
            hasCompletedOnboarding = false
        )

        try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(userProfile)
                .await()
            _uiState.value = LoginUiState(isSuccess = true)
        } catch (e: Exception){
            _uiState.value = LoginUiState(error = e.message)
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank()) {
                onResult(false, "Please enter your email address.")
                return@launch
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.message ?: "Failed to send reset email.")
                    }
                }
        }
    }

    fun startEmailVerificationPolling(
        name: String,
        gender: String,
        age: Int,
        email: String
    ){

        pollingJob?.cancel()

        pollingJob = viewModelScope.launch {
            _emailVerificationState.value = EmailVerificationUiState(isLoading = true)

            while (true) {
                delay(3000)

                try {
                    auth.currentUser?.reload()?.await()
                    if (auth.currentUser?.isEmailVerified == true) {
                        saveVerifiedUserToFirestore(name, gender, age, email)
                        _emailVerificationState.value = EmailVerificationUiState(isVerified = true)
                        break
                    }
                } catch (e: Exception) {
                    // Retry silently and don't update error state permanently
                    println("Email verification polling failed: ${e.message}")
                }
            }
        }
    }

    fun stopEmailVerificationPolling() {
        pollingJob?.cancel()
    }

    // Fail-safe cleanup when the ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                auth.currentUser?.sendEmailVerification()?.await()
                _emailVerificationState.value = _emailVerificationState.value.copy(emailResent = true)
            } catch (e: Exception) {
                _emailVerificationState.value = _emailVerificationState.value.copy(error = e.message)
            }
        }
    }

    fun showError(message: String) {
        _uiState.value = LoginUiState(error = message)
    }

    fun resetError() {
        if (_uiState.value.error != null) {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }

    private fun saveFcmTokenToFirestore(userId: String) = viewModelScope.launch {

        try {
            val token = FirebaseMessaging.getInstance().token.await()
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()
            Log.d("FCM", "Token saved successfully")
        } catch (e: Exception) {
            Log.e("FCM", "Failed to save token", e)
        }
    }
}