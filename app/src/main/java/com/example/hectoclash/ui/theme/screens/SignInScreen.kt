package com.example.hectoclash.ui.theme.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hectoclash.navigation.Routes

import com.example.hectoclash.viewmodels.AuthState
import com.example.hectoclash.viewmodels.AuthViewModel
import com.example.hectoclash.viewmodels.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // isLoading state is now derived from authState.Loading, no need for separate remember
    // var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // Consider using Hilt or a proper dependency injection framework instead of ViewModelFactory long term
    val viewModel: AuthViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading // Derive isLoading directly

    // Handle authentication state changes and navigation
    LaunchedEffect(key1 = authState) {
        when (val state = authState) { // Use 'val state =' for smart casting
            is AuthState.Success -> {
                // Navigate to Home using Routes object
                navController.navigate(Routes.HOME) {
                    // Pop up to SignIn using Routes object
                    popUpTo(Routes.SIGN_IN) { inclusive = true }
                }
                viewModel.resetState() // Reset state after successful navigation
            }
            is AuthState.Error -> {
                // Error message is displayed below, no specific action needed here
                // You could show a Snackbar here as well if preferred
            }
            AuthState.Loading -> {
                // Loading state handled by isLoading variable derived above
            }
            AuthState.Idle -> {
                // No action needed for Idle state
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sign In",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                isError = authState is AuthState.Error // Show error state if auth failed
                // Consider more specific error feedback if needed
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done // Set to Done for the last field
                ),
                singleLine = true,
                isError = authState is AuthState.Error // Show error state if auth failed
            )

            Spacer(modifier = Modifier.height(16.dp)) // Reduced space before error

            // Display error message if authState is Error
            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                // Add placeholder spacer when no error to prevent layout jumps
                Spacer(modifier = Modifier.height(32.dp)) // Height accounts for text + padding
            }


            Button(
                onClick = {
                    // Optionally add validation here before calling viewModel
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.signIn(email, password)
                    } else {
                        // Handle empty fields case if needed, though AuthViewModel also checks
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading // Disable button while loading
            ) {
                // Correct Button Text
                Text("Sign In")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                // Navigate to SignUp using Routes object
                onClick = { navController.navigate(Routes.SIGN_UP) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading // Disable while loading
            ) {
                Text(
                    text = "Don't have an account? Sign Up",
                    textAlign = TextAlign.Center
                )
            }
        } // End Column

        // Loading overlay - shown when authState is Loading
        if (isLoading) {
            // Full screen scrim to block interaction
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)) // Dim background
                    .clickable(enabled = false) { /* Consume clicks */ } ,
                contentAlignment = Alignment.Center
            ) {
                // Centered loading indicator
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
                // Removed the Card around the indicator for simplicity
            }
        } // End Loading Overlay Box

    } // End Outer Box
}