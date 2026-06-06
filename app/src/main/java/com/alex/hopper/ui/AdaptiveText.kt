package com.alex.hopper.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

@Composable
fun cappedSp(
    base: TextUnit,
    maxFontScale: Float = 1.2f,
): TextUnit {
    if (base.type != TextUnitType.Sp) return base
    val systemScale = LocalDensity.current.fontScale
    val effectiveScale = systemScale.coerceAtMost(maxFontScale)
    val adjustedValue = base.value * effectiveScale / systemScale
    return TextUnit(adjustedValue, TextUnitType.Sp)
}

@Composable
fun cappedTextStyle(
    base: TextStyle,
    maxFontScale: Float = 1.2f,
): TextStyle {
    val fontSize = base.fontSize
    return if (fontSize.type == TextUnitType.Sp) {
        base.copy(fontSize = cappedSp(fontSize, maxFontScale))
    } else {
        base
    }
}

@Composable
fun ProvideCappedFontScale(
    maxFontScale: Float = 1.2f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val cappedDensity = Density(
        density = density.density,
        fontScale = density.fontScale.coerceAtMost(maxFontScale),
    )
    CompositionLocalProvider(LocalDensity provides cappedDensity) {
        content()
    }
}
