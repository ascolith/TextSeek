package com.ascolith.textseek.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FabConfig(
    @SerialName("icon_res_id") val iconResId: Int,
    @SerialName("text_res_id") val textResId: Int? = null,
    @SerialName("content_description") val contentDescription: String? = null,
)
