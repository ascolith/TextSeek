package com.ascolith.textseek.feature.overview.ui.model

import androidx.annotation.ColorInt
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId

// todo - remove properties that aren't used by the UI
data class SearchSettingUiModel(
    val id: SearchSettingId = SearchSettingId(0),
    val text: String = "",
    val caseSensitive: Boolean = false,
    @param:ColorInt val colorArgb: Int? = null,
    val enabled: Boolean = true,
    val searchGroupId: SearchGroupId? = null,
)

fun SearchSetting.toUiModel(): SearchSettingUiModel =
    SearchSettingUiModel(
        id = id,
        text = text,
        caseSensitive = caseSensitive,
        colorArgb = color,
        enabled = enabled,
        searchGroupId = searchGroupId,
    )

fun SearchSettingUiModel.toDomain(): SearchSetting =
    SearchSetting(
        id = id,
        text = text,
        caseSensitive = caseSensitive,
        color = colorArgb,
        enabled = enabled,
        searchGroupId = searchGroupId,
    )
