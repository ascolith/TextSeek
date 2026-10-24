package com.ascolith.textseek.feature.camerascreen.ui.model

import androidx.annotation.ColorInt
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId

data class SearchSettingUiModel(
    val id: SearchSettingId,
    val text: String = "",
    val caseSensitive: Boolean = false,
    @param:ColorInt val colorArgb: Int,
    val enabled: Boolean = true,
    val searchGroupId: SearchGroupId? = null,
)

fun SearchSetting.toUiModel(
    isGroupEnabled: Boolean,
    groupColorArgb: Int,
): SearchSettingUiModel =
    SearchSettingUiModel(
        id = id,
        text = text,
        caseSensitive = caseSensitive,
        colorArgb = color ?: groupColorArgb,
        enabled = enabled && isGroupEnabled,
        searchGroupId = searchGroupId,
    )
