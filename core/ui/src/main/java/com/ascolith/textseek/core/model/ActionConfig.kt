package com.ascolith.textseek.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActionConfig(
    @SerialName("icon_res_id") val iconResId: Int,
    @SerialName("label_res_id") val labelResId: Int,
    @SerialName("content_description") val contentDescription: String? = null,
)
