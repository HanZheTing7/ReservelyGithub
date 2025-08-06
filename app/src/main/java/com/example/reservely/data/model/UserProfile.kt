package com.example.reservely.data.model


import java.io.Serializable

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val gender: String = "",
    val age: Int? = 0,
    val profileImageUrl: String = "",
    val email: String = "",
    val isGoogleSignIn: Boolean = false,
    val hasCompletedOnboarding: Boolean = false
)

data class RegistrationInfo(
    val email: String,
    val password: String,
    val name: String,
    val gender: String,
    val age: Int
) : Serializable

