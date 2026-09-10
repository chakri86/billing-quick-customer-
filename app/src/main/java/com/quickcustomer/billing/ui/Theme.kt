package com.quickcustomer.billing.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrandGreen = Color(0xFF246B35)
private val BrandRed = Color(0xFFE31B23)
private val Cream = Color(0xFFFFF8F0)

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EEDB),
    onPrimaryContainer = Color(0xFF0D3B1A),
    secondary = BrandRed,
    background = Cream,
    surface = Color.White,
    surfaceVariant = Color(0xFFF2F4F0),
    outline = Color(0xFF74796F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF91D59D),
    secondary = Color(0xFFFFB4AB),
    background = Color(0xFF111411),
    surface = Color(0xFF181C18)
)

@Composable
fun QuickCustomerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
