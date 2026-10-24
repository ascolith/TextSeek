package com.ascolith.textseek.core.data.searchgroup

import com.ascolith.textseek.core.database.entity.DbSearchGroup
import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId

fun DbSearchGroup.toDomain() =
    SearchGroup(
        id = SearchGroupId(uid),
        name = name,
        enabled = enabled,
        colorArgb = colorArgb,
    )

fun SearchGroup.toDbModel() =
    DbSearchGroup(
        uid = id.value,
        name = name,
        enabled = enabled,
        colorArgb = colorArgb,
    )
