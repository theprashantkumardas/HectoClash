package com.example.hectoclash.ui.theme.screens


import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hectoclash.viewmodels.ProfileViewModel
import com.example.hectoclash.viewmodels.ProfileViewModelFactory


@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(LocalContext.current)),
            onLogout: () -> Unit
) {
    val userName by profileViewModel.userName.collectAsState()
    val userEmail by profileViewModel.userEmail.collectAsState()
    val playerId by profileViewModel.playerId.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Profile", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Name: ${userName ?: "Loading..."}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Email: ${userEmail ?: "Loading..."}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Player ID: ${playerId ?: "Loading..."}", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { profileViewModel.logout { onLogout() } }) {
            Text(text = "Logout")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(
        profileViewModel = TODO(),
        onLogout = TODO()
    )
}