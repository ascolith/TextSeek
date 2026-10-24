package com.ascolith.textseek.feature.overview.ui.model

import com.ascolith.textseek.domain.model.SearchSettingId

data class OverviewItemUiModel(
    val group: SearchGroupUiModel,
    val settings: Map<SearchSettingId, SearchSettingUiModel>,
)
