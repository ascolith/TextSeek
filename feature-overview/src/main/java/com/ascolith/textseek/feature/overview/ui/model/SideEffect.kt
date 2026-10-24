package com.ascolith.textseek.feature.overview.ui.model

import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId

sealed interface SideEffect {
    data object NavigateToSearchGroupAdd : SideEffect
    data class NavigateToSearchGroupEdit(val id: SearchGroupId) : SideEffect
    data class NavigateToSearchSettingAdd(val groupId: SearchGroupId) : SideEffect
    data class NavigateToSearchSettingEdit(val id: SearchSettingId) : SideEffect
}
