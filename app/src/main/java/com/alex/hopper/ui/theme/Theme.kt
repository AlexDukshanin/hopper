package com.alex.hopper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.alex.hopper.settings.AppThemeMode

private val VercelColors = lightColorScheme(
    primary = VercelBlack,
    onPrimary = Color.White,
    primaryContainer = VercelSurfaceRaised,
    onPrimaryContainer = VercelTextPrimary,
    secondary = VercelGraphite,
    onSecondary = Color.White,
    secondaryContainer = VercelSurfaceRaised,
    onSecondaryContainer = VercelTextPrimary,
    tertiary = VercelBlack,
    onTertiary = Color.White,
    error = AppDanger,
    onError = Color.White,
    background = VercelBackground,
    onBackground = VercelTextPrimary,
    surface = VercelSurface,
    onSurface = VercelTextPrimary,
    surfaceContainerHigh = VercelSurfaceRaised,
    onSurfaceVariant = VercelTextSecondary,
    outline = VercelOutline,
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

private val StripeColors = lightColorScheme(
    primary = StripeIndigo,
    onPrimary = Color.White,
    primaryContainer = StripeLavender,
    onPrimaryContainer = StripeTextPrimary,
    secondary = Color(0xFF0A2540),
    onSecondary = Color.White,
    secondaryContainer = StripeSurfaceRaised,
    onSecondaryContainer = StripeTextPrimary,
    tertiary = Color(0xFF00D4FF),
    onTertiary = Color(0xFF06172E),
    error = AppDanger,
    onError = Color.White,
    background = StripeBackground,
    onBackground = StripeTextPrimary,
    surface = StripeSurface,
    onSurface = StripeTextPrimary,
    surfaceContainerHigh = StripeSurfaceRaised,
    onSurfaceVariant = StripeTextSecondary,
    outline = StripeOutline,
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
    error = AppDanger,
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
    error = AppDanger,
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
    themeMode: AppThemeMode = AppThemeMode.HybridClean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = when (themeMode) {
            AppThemeMode.SupabaseDark -> SupabaseDarkColors
            AppThemeMode.StripeStyle -> StripeColors
            AppThemeMode.AirbnbStyle -> AirbnbColors
            AppThemeMode.HybridClean -> HybridCleanColors
        },
        content = content,
    )
}
