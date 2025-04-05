package com.example.hectoclash.ui.theme.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hectoclash.R
import com.example.hectoclash.navigation.Routes
import com.example.hectoclash.viewmodels.AuthState
import com.example.hectoclash.viewmodels.AuthViewModel
import com.example.hectoclash.viewmodels.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var playerId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // isLoading state derived from authState
    // var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading // Derive directly

    // Handle authentication state changes
    LaunchedEffect(key1 = authState) {
        when (val state = authState) { // Use 'val state =' for smart casting
            is AuthState.Success -> {
                // Navigate to Home using Routes object
                navController.navigate(Routes.HOME) {
                    // Pop up appropriately (SignIn should be popped)
                    // Make sure SignInScreen was the previous destination
                    popUpTo(Routes.SIGN_IN) { inclusive = true }
                    // Or if SignUp can be reached from elsewhere, maybe popUpTo the graph start
                    // popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
                viewModel.resetState() // Reset state after successful navigation
            }
            is AuthState.Error -> {
                // Error message is displayed below
            }
            AuthState.Loading -> {
                // Handled by isLoading derived state
            }
            AuthState.Idle -> {
                // No action needed
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content column - make it scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Add vertical scroll
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create Account", // Changed title
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Picture (Placeholder)
            Image(
                painter = painterResource(id = R.drawable.default_profile), // Ensure this drawable exists
                contentDescription = "Default Profile Picture",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(enabled = !isLoading) { /* TODO: Implement image picker */ },
                contentScale = ContentScale.Crop
            )
            Text(
                text = "Tap to choose profile picture",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Input Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name Icon") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                isError = authState is AuthState.Error && name.isEmpty(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = playerId,
                onValueChange = { playerId = it },
                label = { Text("Player ID (Unique)") }, // Added hint
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Player ID Icon") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                isError = authState is AuthState.Error && playerId.isEmpty(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                isError = authState is AuthState.Error && email.isEmpty(),
                enabled = !isLoading
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
                    imeAction = ImeAction.Done // Done action for the last field
                ),
                singleLine = true,
                isError = authState is AuthState.Error && password.isEmpty(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp)) // Reduced space

            // Error Message Display
            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center // Center align error
                )
            } else {
                // Placeholder spacer when no error
                Spacer(modifier = Modifier.height(32.dp)) // Approx height of text + padding
            }

            // Sign Up Button
            Button(
                onClick = {
                    // Optional: Client-side validation before calling viewmodel
                    viewModel.signUp(name, playerId, email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading // Disable button when loading
            ) {
                Text("Sign Up")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Link to Sign In
            TextButton(
                onClick = { navController.popBackStack() }, // Go back to previous screen (SignIn)
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading // Disable while loading
            ) {
                Text(
                    text = "Already have an account? Sign In",
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp)) // Add padding at the bottom
        } // End Column

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)) // Dim background
                    .clickable(enabled = false) { /* Consume clicks */ },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } // End Loading Overlay
    } // End Outer Box
}