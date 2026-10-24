package com.ascolith.textseek.feature.overview.ui.model

import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId

data class SearchGroupUiModel(
    val id: SearchGroupId,
    val name: String,
    val enabled: Boolean,
    val groupColorArgb: Int?,
)

fun SearchGroup.toUiModel() =
    SearchGroupUiModel(
        id = id,
        name = name,
        enabled = enabled,
        groupColorArgb = colorArgb,
    )
