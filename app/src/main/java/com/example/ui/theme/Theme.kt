package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NetflixDarkColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = NetflixWhite,
    primaryContainer = NetflixRedDark,
    onPrimaryContainer = NetflixWhite,
    secondary = NetflixRedLight,
    onSecondary = NetflixWhite,
    tertiary = NetflixGold,
    background = NetflixBlack,
    onBackground = NetflixWhite,
    surface = NetflixDarkGrey,
    onSurface = NetflixWhite,
    surfaceVariant = NetflixCardBg,
    onSurfaceVariant = NetflixOffWhite,
    outline = NetflixCardBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NetflixDarkColorScheme,
        typography = Typography,
        content = content
    )
}
