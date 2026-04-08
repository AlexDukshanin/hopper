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

private val RaycastDarkColors = darkColorScheme(
    primary = RaycastBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF15202B),
    onPrimaryContainer = Color(0xFFD9EEFF),
    secondary = Color(0xFFF9F9F9),
    onSecondary = Color(0xFF101111),
    secondaryContainer = RaycastSurfaceRaised,
    onSecondaryContainer = RaycastTextPrimary,
    tertiary = RaycastRed,
    onTertiary = Color.White,
    error = AppDanger,
    onError = Color.White,
    background = RaycastBackground,
    onBackground = RaycastTextPrimary,
    surface = RaycastSurface,
    onSurface = RaycastTextPrimary,
    surfaceContainerHigh = RaycastSurfaceRaised,
    onSurfaceVariant = RaycastTextSecondary,
    outline = RaycastOutline,
)

private val ComposioDarkColors = darkColorScheme(
    primary = ComposioBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0B2A3F),
    onPrimaryContainer = Color(0xFFD7F1FF),
    secondary = ComposioCyan,
    onSecondary = Color(0xFF062A2A),
    secondaryContainer = ComposioSurfaceRaised,
    onSecondaryContainer = ComposioTextPrimary,
    tertiary = ComposioCyan,
    onTertiary = Color(0xFF062A2A),
    error = AppDanger,
    onError = Color.White,
    background = ComposioBackground,
    onBackground = ComposioTextPrimary,
    surface = ComposioSurface,
    onSurface = ComposioTextPrimary,
    surfaceContainerHigh = ComposioSurfaceRaised,
    onSurfaceVariant = ComposioTextSecondary,
    outline = ComposioOutline,
)

private val NvidiaDarkColors = darkColorScheme(
    primary = NvidiaGreen,
    onPrimary = Color(0xFF101802),
    primaryContainer = Color(0xFF223306),
    onPrimaryContainer = Color(0xFFE1F8A5),
    secondary = NvidiaBlue,
    onSecondary = Color.White,
    secondaryContainer = NvidiaSurfaceRaised,
    onSecondaryContainer = NvidiaTextPrimary,
    tertiary = NvidiaGreen,
    onTertiary = Color(0xFF101802),
    error = AppDanger,
    onError = Color.White,
    background = NvidiaBackground,
    onBackground = NvidiaTextPrimary,
    surface = NvidiaSurface,
    onSurface = NvidiaTextPrimary,
    surfaceContainerHigh = NvidiaSurfaceRaised,
    onSurfaceVariant = NvidiaTextSecondary,
    outline = NvidiaOutline,
)

private val MongoDarkColors = darkColorScheme(
    primary = MongoGreen,
    onPrimary = Color(0xFF032010),
    primaryContainer = Color(0xFF083625),
    onPrimaryContainer = Color(0xFFB9FFD6),
    secondary = MongoBlue,
    onSecondary = Color.White,
    secondaryContainer = MongoSurfaceRaised,
    onSecondaryContainer = MongoTextPrimary,
    tertiary = MongoGreen,
    onTertiary = Color(0xFF032010),
    error = AppDanger,
    onError = Color.White,
    background = MongoBackground,
    onBackground = MongoTextPrimary,
    surface = MongoSurface,
    onSurface = MongoTextPrimary,
    surfaceContainerHigh = MongoSurfaceRaised,
    onSurfaceVariant = MongoTextSecondary,
    outline = MongoOutline,
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
    themeMode: AppThemeMode = AppThemeMode.RaycastDark,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = when (themeMode) {
            AppThemeMode.SupabaseDark -> SupabaseDarkColors
            AppThemeMode.RaycastDark -> RaycastDarkColors
            AppThemeMode.ComposioDark -> ComposioDarkColors
            AppThemeMode.NvidiaDark -> NvidiaDarkColors
            AppThemeMode.MongoDark -> MongoDarkColors
            AppThemeMode.StripeStyle -> StripeColors
            AppThemeMode.HybridClean -> HybridCleanColors
        },
        content = content,
    )
}
