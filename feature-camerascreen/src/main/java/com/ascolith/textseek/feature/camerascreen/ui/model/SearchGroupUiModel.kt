package com.ascolith.textseek.feature.camerascreen.ui.model

import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId

data class SearchGroupUiModel(
    val id: SearchGroupId,
    val name: String,
    val enabled: Boolean,
)

fun SearchGroup.toUiModel() =
    SearchGroupUiModel(
        id = id,
        name = name,
        enabled = enabled,
    )
