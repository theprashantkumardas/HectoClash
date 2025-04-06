package com.example.hectoclashapp.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hectoclash.navigation.Routes

@Composable
fun PlayScreen(mainNavController: NavController) { // Parameter name is fine
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Let's Play!",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp),
            textAlign = TextAlign.Center
        )

        // Use mainNavController to navigate using Routes constants
        ButtonOption(
            text = "Play Online",
            icon = Icons.Filled.Public,
            // Use Routes object here
            onClick = { mainNavController.navigate(Routes.PLAY_ONLINE) },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ButtonOption(
            text = "Play with Friend",
            icon = Icons.Filled.Group,
            // Use Routes object here
            onClick = { mainNavController.navigate(Routes.FRIENDS_LIST) },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ButtonOption(
            text = "Practice",
            icon = Icons.Filled.SportsEsports,
            onClick = { /* TODO: Implement Practice Navigation/Logic (e.g., mainNavController.navigate(Routes.PRACTICE)) */ },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ButtonOption(
            text = "Watch Live Match",
            icon = Icons.Filled.LiveTv,
            onClick = { /* TODO: Implement Spectator Navigation/Logic (e.g., mainNavController.navigate(Routes.SPECTATE)) */ },
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

// ButtonOption Composable (No changes needed)
@Composable
fun ButtonOption(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}