package com.panko.brewmate.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Caramel,          // Buttons in dark mode
    onPrimary = CoffeeDark,     // Text on buttons
    secondary = LatteLight,     // Secondary elements
    tertiary = CoffeeMedium,    // Highlights
    background = CoffeeDark,    // Main screen background
    surface = CoffeeMedium,     // Cards background
    onBackground = Cream,       // Text on background
    onSurface = Cream,           // Text on cards
    secondaryContainer = CoffeeMedium,
    onSecondaryContainer = Cream
)

private val LightColorScheme = lightColorScheme(
    primary = CoffeePrimary,    // Main buttons
    onPrimary = Color.White,    // Text on main buttons
    secondary = CoffeeMedium,   // Secondary accents
    tertiary = Caramel,         // Highlights
    background = Foam,          // Main screen background
    surface = Color.White,      // Cards background
    onBackground = CoffeeDark,  // Text on background
    onSurface = CoffeeDark,      // Text on cards
    secondaryContainer = LatteLight,
    onSecondaryContainer = CoffeeDark
)

@Composable
fun BrewMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}