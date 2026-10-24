package com.ascolith.textseek.feature.searchgroup.ui.model

import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId

sealed interface ScreenIntent {
    data class UpdateSearchGroupName(val name: String) : ScreenIntent
    data object ToggleSearchGroupEnabled : ScreenIntent
    data object DeleteSearchGroup : ScreenIntent
    data object ConfirmDeleteSearchGroup : ScreenIntent
    data object Save : ScreenIntent
    data object Exit : ScreenIntent
    data class ShowDialog(val dialog: TsDialog) : ScreenIntent
    data object HideDialog : ScreenIntent
    data class UpdateColorPickerSelectedColor(val colorArgb: Int?) : ScreenIntent
    data object SaveSearchGroupColor : ScreenIntent
    data class SearchSettingAddClicked(val groupId: SearchGroupId) : ScreenIntent
    data class SearchSettingEditClicked(val id: SearchSettingId) : ScreenIntent
}
