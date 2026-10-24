package com.ascolith.textseek.feature.camerascreen.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId

data class Chip(
    val type: ChipType,
    val text: String,
    val isEnabled: Boolean,
    @param:StringRes val contentDescription: Int,
    @param:DrawableRes val leadingIcon: Int? = null,
    val iconColorArgb: Int? = null,
)

sealed interface ChipType {
    val key: String

    data class SearchSetting(val id: SearchSettingId) : ChipType {
        override val key: String get() = "setting:${id.value}"
    }

    data class SearchGroup(val id: SearchGroupId) : ChipType {
        override val key: String get() = "group:${id.value}"
    }
}
