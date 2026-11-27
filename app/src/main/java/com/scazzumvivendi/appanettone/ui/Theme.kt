package com.scazzumvivendi.appanettone.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.scazzumvivendi.appanettone.R

private val GrayPalette = lightColorScheme(
    primary = Color(0xFF292929),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF292929),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF555555),
    secondaryContainer = Color(0xFFE4E3E3),
    onSecondaryContainer = Color(0xFF222222),
    background = Color(0xFFF7F6F6),
    onBackground = Color(0xFF191919),
    surface = Color(0xFFF7F6F6),
    onSurface = Color(0xFF191919),
    surfaceContainer = Color(0xFFEFEEEE),
    surfaceContainerHigh = Color(0xFFEAE9E9),
    surfaceContainerHighest = Color(0xFFE4E3E3),
    onSurfaceVariant = Color(0xFF505050),
    outline = Color(0xFF777575),
    outlineVariant = Color(0xFFD3D1D1),
)

private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private val AppTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = Poppins),
        bodyMedium = bodyMedium.copy(fontFamily = Poppins),
        bodySmall = bodySmall.copy(fontFamily = Poppins),
        labelLarge = labelLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(fontFamily = Poppins),
    )
}

@Composable
fun AppanettoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GrayPalette,
        typography = AppTypography,
        content = content,
    )
}
