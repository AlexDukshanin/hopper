package com.alex.hopper.data

import androidx.compose.ui.graphics.Color

enum class WagonCondition(
    val shortLabel: String,
) {
    Empty("ПР"),
    Loaded("ГР"),
    Defect("БР"),
    Classifier("КЛ"),
    ;

    fun nextTapState(): WagonCondition = when (this) {
        Empty -> Loaded
        Loaded -> Defect
        Defect -> Empty
        Classifier -> Empty
    }

    fun toggleLongPressState(): WagonCondition = if (this == Classifier) Empty else Classifier

    companion object {
        fun fromStoredValue(value: String?): WagonCondition = when (value) {
            Empty.name, "PR", "ПР" -> Empty
            Loaded.name, "GR", "ГР" -> Loaded
            Defect.name, "BR", "БР" -> Defect
            Classifier.name, "KL", "КЛ" -> Classifier
            else -> Empty
        }
    }
}

val EmptyStateBlue = Color(0xFF2563EB)
val LoadedStateGreen = Color(0xFF16A34A)
val DefectStateRed = Color(0xFFDC2626)
val ClassifierStateYellow = Color(0xFFF59E0B)

fun WagonCondition.containerColor(): Color = when (this) {
    WagonCondition.Empty -> EmptyStateBlue
    WagonCondition.Loaded -> LoadedStateGreen
    WagonCondition.Defect -> DefectStateRed
    WagonCondition.Classifier -> ClassifierStateYellow
}
