package com.alex.hopper.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class HopperUiMetrics(
    val isCompact: Boolean,
    val isLarge: Boolean,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val sectionSpacing: Dp,
    val cardPadding: Dp,
    val smallSpacing: Dp,
    val iconButtonSize: Dp,
    val largeIconButtonSize: Dp,
    val navButtonSize: Dp,
    val primaryActionHeight: Dp,
    val gridMinCardWidth: Dp,
    val screenTitleSize: TextUnit,
    val sectionTitleSize: TextUnit,
    val journalSelectionTopInset: Dp,
)

@Composable
fun rememberHopperUiMetrics(): HopperUiMetrics {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp

    return remember(widthDp) {
        when {
            widthDp < 380 -> HopperUiMetrics(
                isCompact = true,
                isLarge = false,
                horizontalPadding = 12.dp,
                verticalPadding = 12.dp,
                sectionSpacing = 10.dp,
                cardPadding = 14.dp,
                smallSpacing = 6.dp,
                iconButtonSize = 36.dp,
                largeIconButtonSize = 42.dp,
                navButtonSize = 54.dp,
                primaryActionHeight = 52.dp,
                gridMinCardWidth = 148.dp,
                screenTitleSize = 22.sp,
                sectionTitleSize = 18.sp,
                journalSelectionTopInset = 126.dp,
            )

            widthDp < 480 -> HopperUiMetrics(
                isCompact = false,
                isLarge = false,
                horizontalPadding = 16.dp,
                verticalPadding = 16.dp,
                sectionSpacing = 12.dp,
                cardPadding = 18.dp,
                smallSpacing = 8.dp,
                iconButtonSize = 38.dp,
                largeIconButtonSize = 46.dp,
                navButtonSize = 58.dp,
                primaryActionHeight = 56.dp,
                gridMinCardWidth = 160.dp,
                screenTitleSize = 26.sp,
                sectionTitleSize = 20.sp,
                journalSelectionTopInset = 138.dp,
            )

            else -> HopperUiMetrics(
                isCompact = false,
                isLarge = true,
                horizontalPadding = 20.dp,
                verticalPadding = 18.dp,
                sectionSpacing = 14.dp,
                cardPadding = 20.dp,
                smallSpacing = 10.dp,
                iconButtonSize = 40.dp,
                largeIconButtonSize = 48.dp,
                navButtonSize = 62.dp,
                primaryActionHeight = 58.dp,
                gridMinCardWidth = 176.dp,
                screenTitleSize = 30.sp,
                sectionTitleSize = 22.sp,
                journalSelectionTopInset = 146.dp,
            )
        }
    }
}
