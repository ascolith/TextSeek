package com.ascolith.textseek.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_group")
data class DbSearchGroup(
    @ColumnInfo(name = "uid") @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    @ColumnInfo(name = "name") val name: String = "",
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,
    @ColumnInfo(name = "color_argb") val colorArgb: Int,
)
