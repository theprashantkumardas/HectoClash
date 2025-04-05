package com.example.hectoclash.ui.theme.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes // Import the Shapes CLASS
import androidx.compose.ui.unit.dp

// Define an INSTANCE (object) of the Shapes class
val Shapes = Shapes( // This calls the constructor of the Shapes class
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(16.dp), // Make medium more rounded for gamified look (used in Cards)
    large = RoundedCornerShape(24.dp)  // Make large more rounded
)