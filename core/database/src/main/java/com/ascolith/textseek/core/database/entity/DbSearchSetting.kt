package com.ascolith.textseek.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_setting")
data class DbSearchSetting(
    @ColumnInfo(name = "uid") @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    @ColumnInfo(name = "text") val text: String = "",
    @ColumnInfo(name = "case_sensitive") val caseSensitive: Boolean = false,
    @ColumnInfo(name = "color") val color: Int? = null,
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,
    @ColumnInfo(name = "search_group_id") val searchGroupId: Long? = null,
)
