package com.ascolith.textseek.feature.camerascreen.ui.model

import androidx.annotation.ColorInt

sealed interface TsDialog {
    data object None : TsDialog

    data class QuickSearch(
        @param:ColorInt val colorArgb: Int,
        val text: String = "",
        val caseSensitive: Boolean = false,
        @param:ColorInt val colorPickerSelectedColorArgb: Int? = null,
        val isColorPickerVisible: Boolean = false,
    ) : TsDialog
}
