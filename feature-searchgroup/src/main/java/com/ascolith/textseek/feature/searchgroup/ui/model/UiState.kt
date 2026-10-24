package com.ascolith.textseek.feature.searchgroup.ui.model

import com.ascolith.textseek.core.composable.TsSearchSettingItemData
import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId

data class UiState(
    val transientScreen: TransientScreen = TransientScreen.None,
    val dialog: TsDialog = TsDialog.None,
    val id: SearchGroupId? = null,
    val name: String = "",
    val enabled: Boolean = true,
    val colorArgb: Int = SearchGroup.DEFAULT_COLOR_ARGB,
    val searchSettings: Map<SearchSettingId, TsSearchSettingItemData> = emptyMap(),
)
