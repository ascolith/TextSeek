package com.ascolith.textseek.feature.overview.ui.model

import com.ascolith.textseek.domain.model.SearchGroupId

data class UiState(
    val transientScreen: TransientScreen = TransientScreen.None,
    val overviewItems: Map<SearchGroupId, OverviewItemUiModel> = emptyMap(),
)
