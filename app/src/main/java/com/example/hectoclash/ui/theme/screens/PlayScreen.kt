package com.example.hectoclash.ui.theme.screens

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
import com.example.hectoclash.navigation.Screen

@Composable
fun PlayScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Play",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp),
            textAlign = TextAlign.Center
        )


        ButtonOption(
            text = "Play Online",
            icon = Icons.Filled.Public,
            onClick = { navController.navigate(Screen.OnlineUsers.route) },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ButtonOption(
            text = "Play with Friend",
            icon = Icons.Filled.Group,
            onClick = { navController.navigate(Screen.FriendsList.route) },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ButtonOption(
            text = "Practice",
            icon = Icons.Filled.SportsEsports,
            onClick = { /* Handle practice */ },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ButtonOption(
            text = "Watch Live Match",
            icon = Icons.Filled.LiveTv,
            onClick = { /* Handle watch live match */ },
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

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
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}