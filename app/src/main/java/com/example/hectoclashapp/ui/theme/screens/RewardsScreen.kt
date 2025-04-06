package com.example.hectoclashapp.ui.theme.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hectoclash.R

data class Reward(
    val id: Int,
    val title: String,
    val imageRes: Int,
    val coinsRequired: Int,
    val expiryDate: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen() {

    // Dummy data
    val userCoins = 340
    val streakDays = 4 // User has completed 4 out of 7 days

    // Dummy rewards data
    val rewards = listOf(
        Reward(1, "50% Off at Amazon", R.drawable.amzn, 200, "2025-04-30"),
        Reward(2, "Buy 1 Get 1 at Starbucks", R.drawable.starbucks, 150, "2025-05-15"),
        Reward(3, "₹500 off on Swiggy", R.drawable.swiggy, 300, "2025-04-20"),
        Reward(4, "Free Netflix for 1 Month", R.drawable.netflix, 400, "2025-05-10"),
        Reward(5, "15% Off on Uber Rides", R.drawable.uber, 175, "2025-04-25"),
        Reward(6, "Spotify Premium 3 Months", R.drawable.spotify, 350, "2025-05-30")
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

            // Coins display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.coin),
                        contentDescription = "Coins",
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Your Coins",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "$userCoins",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Daily streak - REDUCED HEIGHT
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Practice Streak",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "+10 coins for each day",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        Row {
                            Text(
                                text = "$streakDays/7 days",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (day in 1..7) {
                            DayCircle(
                                day = day,
                                isCompleted = day <= streakDays
                            )
                        }
                    }
                }
            }

            // Rewards heading
            Text(
                text = "Available Rewards",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            // Rewards grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rewards) { reward ->
                    RewardCard(reward = reward, userCoins = userCoins)
                }
            }
        }
    }
}

@Composable
fun DayCircle(day: Int, isCompleted: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)   // Reduced size
                .clip(CircleShape)
                .background(
                    if (isCompleted) Color(0xFFFFD700) else Color.LightGray
                )
                .border(1.dp, Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$day",
                fontSize = 12.sp,  // Smaller text
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) Color.Black else Color.DarkGray
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Day",
            fontSize = 10.sp,  // Smaller text
            color = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardCard(reward: Reward, userCoins: Int) {
    val canRedeem = userCoins >= reward.coinsRequired

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),  // Fixed height to ensure button fits
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),  // Reduced padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Reward image
            Image(
                painter = painterResource(id = reward.imageRes),
                contentDescription = reward.title,
                modifier = Modifier
                    .size(70.dp)  // Reduced size
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )

            // Reward title
            Text(
                text = reward.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,  // Smaller text
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Expiry date
            Text(
                text = "Valid till: ${reward.expiryDate}",
                fontSize = 10.sp,  // Smaller text
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))  // Push button to bottom

            // Coins required
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.coin),
                    contentDescription = "Coins",
                    modifier = Modifier.size(16.dp)  // Smaller icon
                )
                Text(
                    text = " ${reward.coinsRequired}",
                    fontSize = 12.sp,  // Smaller text
                    fontWeight = FontWeight.Bold,
                    color = if (canRedeem)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Gray
                )
            }

            // Redeem button
            Button(
                onClick = { /* Redeem action */ },
                enabled = canRedeem,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),  // Fixed height
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)  // Smaller padding
            ) {
                Text(
                    text = if (canRedeem) "Redeem" else "Not Enough",
                    fontSize = 12.sp  // Smaller text
                )
            }
        }
    }
}