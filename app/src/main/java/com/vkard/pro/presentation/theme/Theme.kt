package com.vkard.pro.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VKardProColorScheme = lightColorScheme(
    primary = Color(0xFF077DF7),          // Brand Blue
    secondary = Color(0xFF2A5D93),        // Brand Blue Dark/Secondary
    background = Color.White,             // White background throughout
    surface = Color.White,                // White surface for cards/inputs
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),     // Dark Slate
    onSurface = Color(0xFF0F172A),        // Dark Slate
    error = Color(0xFFEF4444),            // Red
    onError = Color.White
)

@Composable
fun VKardProTheme(
    darkTheme: Boolean = false,           // Lock out system dark theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VKardProColorScheme,
        content = content
    )
}
