package com.ascolith.textseek.feature.camerascreen.ui.model

import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId

sealed interface SideEffect {
    data class NavigateToSearchSettingEdit(val id: SearchSettingId) : SideEffect
    data class NavigateToSearchGroupEdit(val id: SearchGroupId) : SideEffect
}
