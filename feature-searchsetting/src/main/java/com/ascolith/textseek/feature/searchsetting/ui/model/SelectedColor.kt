package com.ascolith.textseek.feature.searchsetting.ui.model

data class SelectedColor(
    val settingColorArgb: Int? = null,
    val groupColorArgb: Int? = null,
) {

    fun require(): Int = requireNotNull(settingColorArgb ?: groupColorArgb)
}
