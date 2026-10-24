package com.ascolith.textseek.domain.model

data class SearchSetting(
    val id: SearchSettingId,
    val text: String,
    val caseSensitive: Boolean,
    val color: Int?,
    val enabled: Boolean,
    val searchGroupId: SearchGroupId?,
)
