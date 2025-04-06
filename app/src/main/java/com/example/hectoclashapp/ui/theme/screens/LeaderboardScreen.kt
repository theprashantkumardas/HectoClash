package com.example.hectoclashapp.ui.theme.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hectoclash.R

enum class LeaderboardType {
    GLOBAL, COUNTRY, COLLEGE
}

data class LeaderboardEntry(
    val rank: Int,
    val playerId: String,
    val profilePicRes: Int,
    val matches: Int,
    val won: Int,
    val loss: Int,
    val points: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen() {
    // State for dropdown
    var expanded by remember { mutableStateOf(false) }
    var selectedLeaderboardType by remember { mutableStateOf(LeaderboardType.GLOBAL) }

    // Current user and time
    val currentUser = "MonuGit9"

    // Dummy data for user's stats
    val globalRank = 1
    val countryRank = 1
    val collegeRank = 1

    // Dummy data for leaderboard
    val dummyEntries = listOf(
        LeaderboardEntry(1, "Pro_Player1", R.drawable.default_profile, 120, 85, 35, 2500),
        LeaderboardEntry(2, "GameMaster", R.drawable.default_profile, 110, 75, 35, 2350),
        LeaderboardEntry(3, "ChampionGirl", R.drawable.default_profile, 95, 68, 27, 2100),
        LeaderboardEntry(4, "WinnerX", R.drawable.default_profile, 105, 65, 40, 1950),
        LeaderboardEntry(5, "TopPlayer", R.drawable.default_profile, 90, 60, 30, 1800),
        LeaderboardEntry(6, "GamerKid", R.drawable.default_profile, 80, 52, 28, 1650),
        LeaderboardEntry(7, "MonuGit9", R.drawable.default_profile, 75, 48, 27, 1500),
        LeaderboardEntry(8, "LeaderPro", R.drawable.default_profile, 70, 42, 28, 1350),
        LeaderboardEntry(9, "StarGamer", R.drawable.default_profile, 65, 38, 27, 1200),
        LeaderboardEntry(10, "ProGamer123", R.drawable.default_profile, 60, 35, 25, 1050),
    )
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // Global, Country and College Rank all in one row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Global Rank
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GLOBAL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.global),
                        contentDescription = "Global Badge",
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "#$globalRank",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Country Rank
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "COUNTRY",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.country),
                        contentDescription = "Country Badge",
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "#$countryRank",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // College Rank
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "COLLEGE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.college),
                        contentDescription = "College Badge",
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "#$collegeRank",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Dropdown for leaderboard selection with arrow indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedLeaderboardType) {
                                LeaderboardType.GLOBAL -> "Global Leaderboard"
                                LeaderboardType.COUNTRY -> "Country Leaderboard"
                                LeaderboardType.COLLEGE -> "College Leaderboard"
                            },
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown Arrow"
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Global Leaderboard") },
                        onClick = {
                            selectedLeaderboardType = LeaderboardType.GLOBAL
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Country Leaderboard") },
                        onClick = {
                            selectedLeaderboardType = LeaderboardType.COUNTRY
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("College Leaderboard") },
                        onClick = {
                            selectedLeaderboardType = LeaderboardType.COLLEGE
                            expanded = false
                        }
                    )
                }
            }

            // Table Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rank",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(45.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Player ID",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Left
                )
                Text(
                    text = "Matches",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Won",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(45.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Loss",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(45.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Points",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(55.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Leaderboard list
            LazyColumn {
                items(dummyEntries.sortedByDescending { it.points }) { entry ->
                    LeaderboardRow(entry = entry, currentUser = currentUser)
                    Divider(color = Color.LightGray, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun LeaderboardRow(entry: LeaderboardEntry, currentUser: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .background(
                if (entry.playerId == currentUser)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    Color.Transparent
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Text(
            text = "#${entry.rank}",
            modifier = Modifier.width(45.dp),
            textAlign = TextAlign.Center,
            fontWeight = if (entry.rank <= 3) FontWeight.Bold else FontWeight.Normal,
            color = when (entry.rank) {
                1 -> Color(0xFFFFD700) // Gold
                2 -> Color(0xFFC0C0C0) // Silver
                3 -> Color(0xFFCD7F32) // Bronze
                else -> MaterialTheme.colorScheme.onSurface
            }
        )

        // Player ID with profile pic
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = entry.profilePicRes),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.Gray, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = entry.playerId,
                fontWeight = if (entry.playerId == currentUser) FontWeight.Bold else FontWeight.Normal,
                color = if (entry.playerId == currentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Matches
        Text(
            text = "${entry.matches}",
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )

        // Won
        Text(
            text = "${entry.won}",
            modifier = Modifier.width(45.dp),
            textAlign = TextAlign.Center,
            color = Color(0xFF4CAF50)  // Green color for wins
        )

        // Loss
        Text(
            text = "${entry.loss}",
            modifier = Modifier.width(45.dp),
            textAlign = TextAlign.Center,
            color = Color(0xFFF44336)  // Red color for losses
        )

        // Points
        Text(
            text = "${entry.points}",
            modifier = Modifier.width(55.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}