package com.ascolith.textseek.core.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = CURRENT_BACKUP_VERSION,
    val searchGroups: List<BackupSearchGroup>,
    val searchSettings: List<BackupSearchSetting>,
    /** Every [com.ascolith.textseek.domain.model.AppSetting], encoded and keyed by its key. */
    val appSettings: Map<String, String> = emptyMap(),
) {
    companion object {
        const val CURRENT_BACKUP_VERSION = 1
    }
}

@Serializable
data class BackupSearchGroup(
    val uid: Long,
    val name: String,
    val enabled: Boolean,
    val colorArgb: Int,
)

@Serializable
data class BackupSearchSetting(
    val uid: Long,
    val text: String,
    val caseSensitive: Boolean,
    val color: Int?,
    val enabled: Boolean,
    val searchGroupId: Long?,
)
