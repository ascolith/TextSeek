package com.ascolith.textseek.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ascolith.textseek.core.database.entity.DbSearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchSettingDao {
    @Query("SELECT * FROM search_setting ORDER BY uid DESC")
    fun getSearchSettings(): Flow<List<DbSearchSetting>>

    @Query("SELECT * FROM search_setting WHERE uid = :uid")
    suspend fun get(uid: Long): DbSearchSetting?

    @Query("SELECT * FROM search_setting WHERE search_group_id = :groupId ORDER BY uid DESC")
    fun getInGroup(groupId: Long): Flow<List<DbSearchSetting>>

    @Insert
    suspend fun create(item: DbSearchSetting): Long

    @Update
    suspend fun update(item: DbSearchSetting)

    @Query("UPDATE search_setting SET text = :text WHERE uid = :id")
    suspend fun update(id: Long, text: String)

    @Query("DELETE FROM search_setting WHERE uid in (:searchSettingIds)")
    suspend fun delete(searchSettingIds: Set<SearchSettingId>)

    @Query("DELETE FROM search_setting WHERE uid = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM search_setting")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<DbSearchSetting>)
}
