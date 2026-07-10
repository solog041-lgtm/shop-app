package com.example.momosshopmanager.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MomosDarkColorScheme = darkColorScheme(
    primary = MomosOrange,
    onPrimary = DarkBackground,
    primaryContainer = MomosOrangeDark,
    onPrimaryContainer = TextPrimary,
    secondary = ChiliRed,
    onSecondary = TextPrimary,
    secondaryContainer = ChiliRedDark,
    onSecondaryContainer = TextPrimary,
    tertiary = Golden,
    onTertiary = DarkBackground,
    tertiaryContainer = GoldenDark,
    onTertiaryContainer = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextPrimary,
    outline = TextMuted,
    outlineVariant = DarkCardElevated,
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = MomosOrangeDark,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkCard,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = DarkCardElevated,
)

@Composable
fun MomosShopManagerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MomosDarkColorScheme,
        typography = MomosTypography,
        content = content
    )
}
