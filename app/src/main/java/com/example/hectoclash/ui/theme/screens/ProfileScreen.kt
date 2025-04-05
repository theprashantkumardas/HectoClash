package com.example.hectoclash.ui.theme.screens


import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hectoclash.navigation.Routes
import com.example.hectoclash.utils.SocketManager
import com.example.hectoclash.viewmodels.ProfileViewModel
import com.example.hectoclash.viewmodels.ProfileViewModelFactory


@Composable
fun ProfileScreen(
    nestedNavController: NavController,
    mainNavController: NavController, // For logout
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(LocalContext.current)),
    // Removed onLogout lambda, handle directly in VM or here
//    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(LocalContext.current)),
    onLogout: () -> Unit
) {
    val userName by profileViewModel.userName.collectAsState()
    val userEmail by profileViewModel.userEmail.collectAsState()
    val playerId by profileViewModel.playerId.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "My Profile", style = MaterialTheme.typography.headlineMedium)
        Icon(Icons.Filled.AccountCircle, contentDescription = "My Avatar", modifier = Modifier.size(60.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Name: ${userName ?: "Loading..."}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Email: ${userEmail ?: "Loading..."}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Player ID: ${playerId ?: "Loading..."}", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        // TODO: Display stats (Wins, Losses, etc.) here - requires fetching

        // Button to go to Friends List
        Button(onClick = { nestedNavController.navigate(Routes.FRIENDS_LIST) }) {
            Icon(Icons.Default.People, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("View Friends & Requests")
        }

        Spacer(modifier = Modifier.height(16.dp)) // Add space before logout

        Button(onClick = { profileViewModel.logout { onLogout() } }) {
            Text(text = "Logout")
        }

        // Logout Button
        Button(
            onClick = {
                profileViewModel.logout {
                    // Use mainNavController to navigate outside HOME scope
                    SocketManager.disconnect() // Ensure socket disconnects on logout
                    mainNavController.navigate(Routes.SIGN_IN) {
                        popUpTo(Routes.HOME) { inclusive = true } // Clear back stack up to Home
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(text = "Logout")
        }
    }

}

//@Preview(showBackground = true)
//@Composable
//fun ProfileScreenPreview() {
//    ProfileScreen(
//        profileViewModel = TODO(),
//        onLogout = TODO()
//    )
//}