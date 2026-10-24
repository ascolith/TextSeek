package com.ascolith.textseek.feature.searchsetting.ui.model

sealed interface TsDialog {
    data object None : TsDialog
    data class ColorPicker(val selectedColorArgb: Int? = null) : TsDialog
    data object GroupPicker : TsDialog
    data object DeleteConfirmation : TsDialog
}
