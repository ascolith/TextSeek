package com.ascolith.textseek.feature.overview.ui.model

import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId

sealed interface ScreenIntent {
    data object SearchGroupAddClicked : ScreenIntent
    data class SearchGroupEditClicked(val id: SearchGroupId) : ScreenIntent
    data class ToggleSearchGroupEnabled(val id: SearchGroupId) : ScreenIntent
    data class SearchSettingAddClicked(val groupId: SearchGroupId) : ScreenIntent
    data class SearchSettingEditClicked(val id: SearchSettingId) : ScreenIntent
}
