package com.example.reservely.presentation.features.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.reservely.data.model.RegistrationInfo
import com.example.reservely.presentation.components.CustomButton
import com.example.reservely.presentation.components.CustomOutlinedTextField
import com.example.reservely.presentation.components.GenderSelector

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: LoginScreenViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.verificationSent) {
        if (uiState.verificationSent) {
            val registrationInfo = RegistrationInfo(email, password, name, selectedCategory, age.toInt())
            navController.currentBackStackEntry?.savedStateHandle?.set("registrationInfo", registrationInfo)
            navController.navigate("emailVerification")
            viewModel.resetState()
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateToHome()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(24.dp))

        CustomOutlinedTextField(value = name, onValueChange = { name = it }, label = "Full Name")

        Spacer(modifier = Modifier.height(16.dp))

        GenderSelector(
            selectedCategory = selectedCategory,
            onCategorySelected = {
                selectedCategory = it
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomOutlinedTextField(value = age, onValueChange = { age = it }, label = "Age", keyboardOptions = KeyboardType.Number)
        CustomOutlinedTextField(value = email, onValueChange = { email = it }, label = "Email", keyboardOptions = KeyboardType.Email)
        CustomOutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true,
            keyboardOptions = KeyboardType.Password
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        CustomButton(
            text = "Create Account",
            isLoading = uiState.isLoading,
            onClick = {
                val parsedAge = age.toIntOrNull()
                if (
                    email.isNotBlank() &&
                    password.length >= 6 &&
                    name.isNotBlank() &&
                    parsedAge != null &&
                    selectedCategory.isNotBlank()
                ) {
                    viewModel.registerWithEmail(
                        email = email,
                        password = password,
                        name = name,
                        gender = selectedCategory,
                        age = parsedAge
                    )
                } else {
                    Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
        )


        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Sign in")
        }
    }
}
