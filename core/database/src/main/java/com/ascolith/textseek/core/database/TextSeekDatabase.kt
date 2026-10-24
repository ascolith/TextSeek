package com.ascolith.textseek.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ascolith.textseek.core.database.dao.SearchGroupDao
import com.ascolith.textseek.core.database.dao.SearchSettingDao
import com.ascolith.textseek.core.database.entity.DbSearchGroup
import com.ascolith.textseek.core.database.entity.DbSearchSetting

@Database(
    entities = [
        DbSearchSetting::class,
        DbSearchGroup::class,
    ],
    version = 1
)
abstract class TextSeekDatabase : RoomDatabase() {
    abstract fun searchGroupDao(): SearchGroupDao
    abstract fun searchSettingDao(): SearchSettingDao
}
