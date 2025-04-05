// src/main/java/com/example/hectoclash/ui/theme/Theme.kt
package com.example.hectoclash.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color // Import Color explicitly
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.hectoclash.ui.theme.components.Shapes

// Define custom dark colors based on previous request
private val DarkHectoColorScheme = darkColorScheme(
    primary = GreenAccent,         // Bright Green for primary actions/highlights
    onPrimary = Color.Black,       // Text/icons on primary color (green)

    secondary = PurpleFriend,      // Example: Use purple for friend icons? (Adjust as needed)
    onSecondary = Color.White,

    tertiary = ProfilePink,        // Example: Use pink for profile icon bg? (Adjust as needed)
    onTertiary = Color.White,

    background = DarkBackgroundEnd, // Near Black for main screen background
    onBackground = TextOnDark,     // White text on dark background

    surface = DarkSurface,         // Dark Grey for cards, dialogs, surfaces
    onSurface = TextOnDark,        // White text on dark surfaces

    surfaceVariant = Color(0xFF3A3A3A), // A slightly different shade for variety
    onSurfaceVariant = TextOnDarkSecondary, // Grey text on surface variants

    error = Color(0xFFCF6679),     // Standard Material dark error color
    onError = Color.Black,

    // You can customize other colors like inversePrimary, scrim etc. if needed
    // secondaryContainer = ...,
    // onSecondaryContainer = ...,
    // tertiaryContainer = ...,
    // onTertiaryContainer = ...,
    // errorContainer = ...,
    // onErrorContainer = ...,
    // inverseSurface = ...,
    // inverseOnSurface = ...,
    // outline = ...,
    // outlineVariant = ...,
    // scrim = ...,
)

// Define a basic corresponding light theme (optional, adjust as needed)
// For now, it's just the default light scheme - customize if you plan to support light mode.
private val LightHectoColorScheme = lightColorScheme(
    primary = GreenAccent, // Use green accent in light mode too?
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White, // Text on green might need adjusting
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    // ... other default light colors
)

@Composable
fun HectoClashTheme(
    // Force dark theme as per the design focus
    darkTheme: Boolean = true,
    // Disable dynamic color to enforce the custom black/green theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color is disabled by default now, but kept for flexibility
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Use custom Dark scheme if darkTheme is true
        darkTheme -> DarkHectoColorScheme
        // Use Light scheme otherwise (currently default, customize if needed)
        else -> LightHectoColorScheme // Or your custom LightHectoColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar color to match the dark background for seamless look
            window.statusBarColor = colorScheme.background.toArgb()
            // Set status bar icons/text to light for dark background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // Similarly, you might want to control the navigation bar color/contrast:
            // window.navigationBarColor = colorScheme.background.toArgb()
            // WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Ensure you have Typography defined
        shapes = Shapes,       // Ensure you have Shapes defined
        content = content
    )
}