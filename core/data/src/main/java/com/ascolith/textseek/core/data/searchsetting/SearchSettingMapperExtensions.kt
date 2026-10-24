package com.ascolith.textseek.core.data.searchsetting

import com.ascolith.textseek.core.database.entity.DbSearchSetting
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId

fun DbSearchSetting.toDomain() =
    SearchSetting(
        id = SearchSettingId(uid),
        text = text,
        caseSensitive = caseSensitive,
        color = color,
        enabled = enabled,
        searchGroupId = searchGroupId?.let { SearchGroupId(it) },
    )

fun SearchSetting.toDbModel() =
    DbSearchSetting(
        uid = id.value,
        text = text,
        caseSensitive = caseSensitive,
        color = color,
        enabled = enabled,
        searchGroupId = searchGroupId?.value,
    )
