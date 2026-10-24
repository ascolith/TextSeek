package com.ascolith.textseek.feature.camerascreen.ui.model

import androidx.annotation.ColorInt
import com.ascolith.textseek.domain.model.SearchSettingId

data class QuickSearchUiModel(
    val text: String,
    val caseSensitive: Boolean,
    @param:ColorInt val colorArgb: Int,
) {
    companion object {
        val QUICK_SEARCH_SETTING_ID = SearchSettingId(-1L)
    }
}

fun QuickSearchUiModel.toSearchSettingUiModel(): SearchSettingUiModel =
    SearchSettingUiModel(
        id = QuickSearchUiModel.QUICK_SEARCH_SETTING_ID,
        text = text,
        caseSensitive = caseSensitive,
        colorArgb = colorArgb,
        enabled = true,
        searchGroupId = null,
    )
