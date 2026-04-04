package com.alex.xdw.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.alex.xdw.settings.AppThemeMode

private val AppleLightColors = lightColorScheme(
    primary = AppleBlue,
    onPrimary = Color.White,
    primaryContainer = AppleBlueSoft,
    onPrimaryContainer = AppleTextPrimary,
    secondary = Color(0xFF111111),
    onSecondary = Color.White,
    secondaryContainer = AppleSurfaceRaised,
    onSecondaryContainer = AppleTextPrimary,
    tertiary = Color(0xFF5E5CE6),
    onTertiary = Color.White,
    error = AppleDanger,
    onError = Color.White,
    background = AppleBackground,
    onBackground = AppleTextPrimary,
    surface = AppleSurface,
    onSurface = AppleTextPrimary,
    surfaceContainerHigh = AppleSurfaceRaised,
    onSurfaceVariant = AppleTextSecondary,
    outline = AppleOutline,
)

private val SupabaseDarkColors = darkColorScheme(
    primary = SupabaseGreen,
    onPrimary = Color(0xFF05140C),
    primaryContainer = Color(0xFF153324),
    onPrimaryContainer = Color(0xFFB8F3D3),
    secondary = Color(0xFFFAFAFA),
    onSecondary = Color(0xFF0F0F0F),
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = SupabaseTextPrimary,
    tertiary = SupabaseGreenDeep,
    onTertiary = Color(0xFF05140C),
    error = SupabaseDanger,
    onError = Color.White,
    background = SupabaseBackground,
    onBackground = SupabaseTextPrimary,
    surface = SupabaseSurface,
    onSurface = SupabaseTextPrimary,
    surfaceContainerHigh = SupabaseSurfaceRaised,
    onSurfaceVariant = SupabaseTextSecondary,
    outline = SupabaseOutline,
)

private val AirtableLightColors = lightColorScheme(
    primary = AirtableBlue,
    onPrimary = Color.White,
    primaryContainer = AirtableCyan,
    onPrimaryContainer = AirtableTextPrimary,
    secondary = Color(0xFF334155),
    onSecondary = Color.White,
    secondaryContainer = AirtableSurfaceRaised,
    onSecondaryContainer = AirtableTextPrimary,
    tertiary = Color(0xFF00A6C7),
    onTertiary = Color.White,
    error = AppleDanger,
    onError = Color.White,
    background = AirtableBackground,
    onBackground = AirtableTextPrimary,
    surface = AirtableSurface,
    onSurface = AirtableTextPrimary,
    surfaceContainerHigh = AirtableSurfaceRaised,
    onSurfaceVariant = AirtableTextSecondary,
    outline = AirtableOutline,
)

private val FigmaLightColors = lightColorScheme(
    primary = FigmaPurple,
    onPrimary = Color.White,
    primaryContainer = FigmaMint,
    onPrimaryContainer = FigmaTextPrimary,
    secondary = Color(0xFF111827),
    onSecondary = Color.White,
    secondaryContainer = FigmaSurfaceRaised,
    onSecondaryContainer = FigmaTextPrimary,
    tertiary = Color(0xFF18A0FB),
    onTertiary = Color.White,
    error = AppleDanger,
    onError = Color.White,
    background = FigmaBackground,
    onBackground = FigmaTextPrimary,
    surface = FigmaSurface,
    onSurface = FigmaTextPrimary,
    surfaceContainerHigh = FigmaSurfaceRaised,
    onSurfaceVariant = FigmaTextSecondary,
    outline = FigmaOutline,
)

private val AirbnbColors = lightColorScheme(
    primary = AirbnbCoral,
    onPrimary = Color.White,
    primaryContainer = AirbnbRose,
    onPrimaryContainer = AirbnbTextPrimary,
    secondary = Color(0xFF484848),
    onSecondary = Color.White,
    secondaryContainer = AirbnbSurfaceRaised,
    onSecondaryContainer = AirbnbTextPrimary,
    tertiary = Color(0xFFFF385C),
    onTertiary = Color.White,
    error = AppleDanger,
    onError = Color.White,
    background = AirbnbBackground,
    onBackground = AirbnbTextPrimary,
    surface = AirbnbSurface,
    onSurface = AirbnbTextPrimary,
    surfaceContainerHigh = AirbnbSurfaceRaised,
    onSurfaceVariant = AirbnbTextSecondary,
    outline = AirbnbOutline,
)

private val HybridCleanColors = lightColorScheme(
    primary = HybridBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = HybridTextPrimary,
    secondary = HybridGreen,
    onSecondary = Color.White,
    secondaryContainer = HybridSurfaceRaised,
    onSecondaryContainer = HybridTextPrimary,
    tertiary = HybridGreen,
    onTertiary = Color.White,
    error = AppleDanger,
    onError = Color.White,
    background = HybridBackground,
    onBackground = HybridTextPrimary,
    surface = HybridSurface,
    onSurface = HybridTextPrimary,
    surfaceContainerHigh = HybridSurfaceRaised,
    onSurfaceVariant = HybridTextSecondary,
    outline = HybridOutline,
)

@Composable
fun XdwTheme(
    themeMode: AppThemeMode = AppThemeMode.AppleLight,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = when (themeMode) {
            AppThemeMode.AppleLight -> AppleLightColors
            AppThemeMode.SupabaseDark -> SupabaseDarkColors
            AppThemeMode.AirtableLight -> AirtableLightColors
            AppThemeMode.FigmaLight -> FigmaLightColors
            AppThemeMode.AirbnbStyle -> AirbnbColors
            AppThemeMode.HybridClean -> HybridCleanColors
        },
        content = content,
    )
}
