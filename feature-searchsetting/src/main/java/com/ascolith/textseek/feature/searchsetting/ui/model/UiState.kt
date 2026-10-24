package com.ascolith.textseek.feature.searchsetting.ui.model

import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchSettingId

data class UiState(
    val transientScreen: TransientScreen = TransientScreen.None,
    val dialog: TsDialog = TsDialog.None,
    val searchGroups: List<SearchGroup> = emptyList(),
    val selectedSearchGroup: SearchGroup? = null,

    val id: SearchSettingId? = null,
    val text: String = "",
    val caseSensitive: Boolean = false,
    val selectedColor: SelectedColor = SelectedColor(),
    val enabled: Boolean = true,
)
