package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SoftSlatePrimaryDark,
    onPrimary = SoftSlateOnPrimaryDark,
    primaryContainer = SoftSlateContainerDark,
    onPrimaryContainer = SoftSlateOnContainerDark,
    secondary = SoftSageSecondaryDark,
    onSecondary = SoftSageOnSecondaryDark,
    secondaryContainer = SoftSageContainerDark,
    onSecondaryContainer = SoftSageOnContainerDark,
    tertiary = SoftCoralTertiaryDark,
    onTertiary = SoftCoralContainerDark,
    background = SoftBackgroundDark,
    onBackground = SoftOnBackgroundDark,
    surface = SoftSurfaceDark,
    onSurface = SoftOnSurfaceDark,
    surfaceVariant = SoftSurfaceVariantDark,
    onSurfaceVariant = SoftOnSurfaceVariantDark,
    outline = SoftOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = SoftSlatePrimaryLight,
    onPrimary = SoftSlateOnPrimaryLight,
    primaryContainer = SoftSlateContainerLight,
    onPrimaryContainer = SoftSlateOnContainerLight,
    secondary = SoftSageSecondaryLight,
    onSecondary = SoftSageOnSecondaryLight,
    secondaryContainer = SoftSageContainerLight,
    onSecondaryContainer = SoftSageOnContainerLight,
    tertiary = SoftCoralTertiaryLight,
    onTertiary = SoftCoralOnContainerLight,
    background = SoftBackgroundLight,
    onBackground = SoftOnBackgroundLight,
    surface = SoftSurfaceLight,
    onSurface = SoftOnSurfaceLight,
    surfaceVariant = SoftSurfaceVariantLight,
    onSurfaceVariant = SoftOnSurfaceVariantLight,
    outline = SoftOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our carefully tuned soft palette by default to avoid eye strain
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
