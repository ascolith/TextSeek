package com.ascolith.textseek.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ascolith.textseek.core.database.entity.DbSearchGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchGroupDao {
    @Query("SELECT * FROM search_group ORDER BY uid DESC")
    fun getSearchGroups(): Flow<List<DbSearchGroup>>

    @Query("SELECT * FROM search_group WHERE uid = :uid")
    suspend fun get(uid: Long): DbSearchGroup?

    @Insert
    suspend fun create(item: DbSearchGroup): Long

    @Update
    suspend fun update(item: DbSearchGroup)

    @Query(
        """
            UPDATE search_group
            SET enabled = :enabled
            WHERE uid = :id
        """
    )
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query(
        """
        DELETE FROM search_group
        WHERE uid = :id
        """
    )
    suspend fun delete(id: Long)

    @Query(
        """
        DELETE FROM search_setting
        WHERE search_group_id = :id
        """
    )
    suspend fun deleteGroupChildren(id: Long)

    @Transaction
    suspend fun deleteGroupAndChildren(id: Long) {
        delete(id)
        deleteGroupChildren(id)
    }

    @Query("DELETE FROM search_group")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<DbSearchGroup>)
}
