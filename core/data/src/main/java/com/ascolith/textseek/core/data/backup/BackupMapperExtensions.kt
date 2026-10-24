package com.ascolith.textseek.core.data.backup

import com.ascolith.textseek.core.data.backup.model.BackupSearchGroup
import com.ascolith.textseek.core.data.backup.model.BackupSearchSetting
import com.ascolith.textseek.core.database.entity.DbSearchGroup
import com.ascolith.textseek.core.database.entity.DbSearchSetting

fun DbSearchGroup.toBackup() =
    BackupSearchGroup(
        uid = uid,
        name = name,
        enabled = enabled,
        colorArgb = colorArgb,
    )

fun DbSearchSetting.toBackup() =
    BackupSearchSetting(
        uid = uid,
        text = text,
        caseSensitive = caseSensitive,
        color = color,
        enabled = enabled,
        searchGroupId = searchGroupId,
    )

fun BackupSearchGroup.toDbModel() =
    DbSearchGroup(
        uid = uid,
        name = name,
        enabled = enabled,
        colorArgb = colorArgb,
    )

fun BackupSearchSetting.toDbModel() =
    DbSearchSetting(
        uid = uid,
        text = text,
        caseSensitive = caseSensitive,
        color = color,
        enabled = enabled,
        searchGroupId = searchGroupId,
    )
