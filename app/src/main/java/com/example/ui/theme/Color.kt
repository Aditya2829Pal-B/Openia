package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Futuristic Dark Palette
val DarkCosmicBlack = Color(0xFF0B0E14)
val DarkCosmicDark = Color(0xFF131722)
val DarkCosmicGray = Color(0xFF1E2433)
val DarkCosmicInput = Color(0xFF161A24)
val DarkNeoCyan = Color(0xFF00E5FF)
val DarkSolarCoral = Color(0xFFFF5722)
val DarkSoftText = Color(0xFF90A4AE)
val DarkSoftBorder = Color(0xFF37474F)
val DarkHeaderText = Color(0xFFECEFF1)
val DarkAccentPurple = Color(0xFF7C4DFF)

// Futuristic Light Palette
val LightBackground = Color(0xFFF5F7FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE4E7EB)
val LightSurfaceInput = Color(0xFFF0F4F8)
val LightPrimary = Color(0xFF0097A7) // Darker cyan for light mode
val LightSecondary = Color(0xFF651FFF) 
val LightTertiary = Color(0xFFE64A19) // Darker coral for light mode
val LightText = Color(0xFF1A1F2B)
val LightSoftText = Color(0xFF4A5568)
val LightBorder = Color(0xFFCBD5E1)

// Dynamic Colors
val CosmicBlack: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkCosmicBlack else LightBackground

val CosmicDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkCosmicDark else LightSurface

val CosmicGray: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkCosmicGray else LightSurfaceVariant

val CosmicInput: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkCosmicInput else LightSurfaceInput

val NeoCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkNeoCyan else LightPrimary

val SolarCoral: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkSolarCoral else LightTertiary

val SoftText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkSoftText else LightSoftText

val SoftBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkSoftBorder else LightBorder

val HeaderText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkHeaderText else LightText

val AccentPurple: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkAccentPurple else LightSecondary
