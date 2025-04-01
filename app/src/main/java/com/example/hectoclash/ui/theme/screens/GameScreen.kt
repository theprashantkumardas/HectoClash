package com.example.hectoclash.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.hectoclash.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    opponentId: String,
    opponentName: String,
    opponentPoints: Int
) {
    // Generate a 6-digit number for the game
    val digits = remember { List(6) { (0..9).random() } }
    val digitString = remember { digits.joinToString("") }

    // State for the user's answer
    var userAnswer by remember { mutableStateOf(digitString) }

    // Timer state
    var remainingTime by remember { mutableStateOf(60) }
    val formattedTime = remember(remainingTime) {
        String.format("%02d:%02d", remainingTime / 60, remainingTime % 60)
    }

    // Current question state
    val currentQuestion = 1
    val totalQuestions = 10

    // Timer effect
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(key1 = true) {
        coroutineScope.launch {
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Math Game") },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back */ }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Top section with player profiles and game info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Your profile
                PlayerProfile(
                    imageUrl = "https://static.vecteezy.com/system/resources/thumbnails/054/078/735/small_2x/gamer-avatar-with-headphones-and-controller-vector.jpg", // Replace with actual URL
                    name = "You",
                    points = 1500
                )

                // Timer and question counter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingTime <= 10) Color.Red else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$currentQuestion/$totalQuestions",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Opponent profile
                PlayerProfile(
                    imageUrl = "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", // Replace with actual URL
                    name = opponentName,
                    points = opponentPoints
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Game content
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Insert operators between digits to create a valid expression:",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = digitString,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = userAnswer,
                        onValueChange = { userAnswer = it },
                        label = { Text("Insert operators (e.g., 1+2*3-4/5+6)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { /* Handle submission */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerProfile(
    imageUrl: String,
    name: String,
    points: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Profile picture of $name",
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.default_profile)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "$points pts",
            style = MaterialTheme.typography.bodySmall
        )
    }
}