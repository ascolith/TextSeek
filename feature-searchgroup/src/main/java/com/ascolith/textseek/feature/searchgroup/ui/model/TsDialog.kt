package com.ascolith.textseek.feature.searchgroup.ui.model

sealed interface TsDialog {
    data object None : TsDialog
    data object DeleteConfirmation : TsDialog
    data class ColorPicker(val selectedColorArgb: Int? = null) : TsDialog
}
