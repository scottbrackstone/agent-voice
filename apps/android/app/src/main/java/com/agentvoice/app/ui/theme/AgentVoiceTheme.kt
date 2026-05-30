package com.agentvoice.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6F73),
    onPrimary = Color.White,
    secondary = Color(0xFF6A5F2A),
    tertiary = Color(0xFF8A4B68),
    background = Color(0xFFF8FAF9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE4ECEA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8ACDD0),
    secondary = Color(0xFFD6C56D),
    tertiary = Color(0xFFE4A7C5),
    background = Color(0xFF101414),
    surface = Color(0xFF171D1D),
    surfaceVariant = Color(0xFF334142)
)

@Composable
fun AgentVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

