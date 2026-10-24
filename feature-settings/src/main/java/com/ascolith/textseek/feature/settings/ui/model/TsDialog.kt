package com.ascolith.textseek.feature.settings.ui.model

sealed interface TsDialog {
    data object None : TsDialog
    data object ImportConfirmation : TsDialog
    data class GlobalDefaultIndicatorColorPicker(val selectedColorArgb: Int? = null) : TsDialog
    data object TextRecognitionScriptPicker : TsDialog
}
