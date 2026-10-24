package com.ascolith.textseek.feature.searchsetting.ui.model

import com.ascolith.textseek.domain.model.SearchGroupId

sealed interface ScreenIntent {
    data class UpdateSearchSettingText(val text: String) : ScreenIntent
    data object SaveSearchSettingColor : ScreenIntent
    data object UseGroupColor : ScreenIntent
    data object ToggleSearchSettingCaseSensitive : ScreenIntent
    data object ToggleSearchSettingEnabled : ScreenIntent
    data class UpdateSearchSettingGroup(val searchGroupId: SearchGroupId?) : ScreenIntent
    data object DeleteSearchSetting : ScreenIntent
    data object ConfirmDeleteSearchSetting : ScreenIntent
    data object Save : ScreenIntent
    data object Exit : ScreenIntent
    data class ShowDialog(val dialog: TsDialog) : ScreenIntent
    data object HideDialog : ScreenIntent
    data class UpdateColorPickerSelectedColor(val colorArgb: Int) : ScreenIntent
}
