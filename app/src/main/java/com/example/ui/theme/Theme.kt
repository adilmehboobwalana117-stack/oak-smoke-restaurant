package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val OakAndSmokeColorScheme = darkColorScheme(
    primary = SmokedAmber,
    onPrimary = CharcoalDeep,
    primaryContainer = SmokedAmberDeep,
    onPrimaryContainer = SmokedAmberBright,
    secondary = TandoorCopper,
    onSecondary = ParchmentCream,
    secondaryContainer = TandoorCopperSoft,
    onSecondaryContainer = SmokedAmberBright,
    tertiary = CorianderGreen,
    onTertiary = CharcoalDeep,
    tertiaryContainer = CorianderGreenDark,
    onTertiaryContainer = CorianderGreen,
    error = CrimsonEmber,
    onError = ParchmentCream,
    errorContainer = CrimsonEmberDark,
    onErrorContainer = CrimsonEmber,
    background = CharcoalDeep,
    onBackground = ParchmentCream,
    surface = CharcoalSurface,
    onSurface = ParchmentCream,
    surfaceVariant = CharcoalContainer,
    onSurfaceVariant = WarmAsh,
    surfaceContainerLowest = CharcoalDeep,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = CharcoalContainer,
    surfaceContainerHigh = CharcoalContainerHigh,
    surfaceContainerHighest = EmberBorder,
    outline = EmberBorder,
    outlineVariant = CharcoalContainerHigh
)

private val OakAndSmokeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OakAndSmokeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OakAndSmokeColorScheme,
        typography = Typography,
        shapes = OakAndSmokeShapes,
        content = content
    )
}
