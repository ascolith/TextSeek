package com.ascolith.textseek.feature.searchgroup.ui.model

import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId

sealed interface SideEffect {
    data object Exit : SideEffect
    data class NavigateToSearchSettingAdd(val groupId: SearchGroupId) : SideEffect
    data class NavigateToSearchSettingEdit(val settingId: SearchSettingId) : SideEffect
}
