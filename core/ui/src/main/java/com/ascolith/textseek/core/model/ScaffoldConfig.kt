package com.ascolith.textseek.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScaffoldConfig(
    @SerialName("top_bar_title_res_id") val topBarTitleResId: Int? = null,
    @SerialName("fab_config") val fabConfig: FabConfig? = null,
    @SerialName("show_bottom_bar") val showBottomBar: Boolean = true,
    @SerialName("show_back_button") val showBackButton: Boolean = false,
    @SerialName("actions") val actions: List<ActionConfig> = emptyList(),
)
