package com.ascolith.textseek.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ascolith.textseek.core.database.dao.SearchSettingDao
import com.ascolith.textseek.core.database.entity.DbSearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented integration tests for [SearchSettingDao] using Room.
 */
@RunWith(AndroidJUnit4::class)
class SearchSettingDaoInstrumentedTest {

    private lateinit var database: TextSeekDatabase
    private lateinit var searchSettingDao: SearchSettingDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TextSeekDatabase::class.java)
            .allowMainThreadQueries() // For testing only
            .build()
        searchSettingDao = database.searchSettingDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetSearchSetting() = runBlocking {
        val searchSetting = DbSearchSetting(text = "instrumented", caseSensitive = true, color = 0x123456)
        val id = searchSettingDao.create(searchSetting)
        val retrieved = searchSettingDao.get(id)
        assertEquals(searchSetting.text, retrieved?.text)
        assertEquals(searchSetting.caseSensitive, retrieved?.caseSensitive)
        assertEquals(searchSetting.color, retrieved?.color)
    }

    @Test
    fun updateSearchSetting() = runBlocking {
        val searchSetting = DbSearchSetting(text = "to update", caseSensitive = false, color = 0x111111)
        val id = searchSettingDao.create(searchSetting)
        val updated = DbSearchSetting(uid = id, text = "updated", caseSensitive = true, color = 0x222222)
        searchSettingDao.update(updated)
        val retrieved = searchSettingDao.get(id)
        assertEquals("updated", retrieved?.text)
        assertEquals(true, retrieved?.caseSensitive)
        assertEquals(0x222222, retrieved?.color)
    }

    @Test
    fun updateSearchSettingText_onlyTextChanges() = runBlocking {
        val searchSetting = DbSearchSetting(text = "old text", caseSensitive = true, color = 0x333333)
        val id = searchSettingDao.create(searchSetting)
        searchSettingDao.update(id, "new text")
        val retrieved = searchSettingDao.get(id)
        assertEquals("new text", retrieved?.text)
        assertEquals(true, retrieved?.caseSensitive)
        assertEquals(0x333333, retrieved?.color)
    }

    @Test
    fun deleteSearchSetting() = runBlocking {
        val searchSetting = DbSearchSetting(text = "to delete", caseSensitive = false)
        val id = searchSettingDao.create(searchSetting)
        searchSettingDao.delete(id)
        val retrieved = searchSettingDao.get(id)
        assertNull(retrieved)
    }

    @Test
    fun getSearchSettings_returnsAllInDescendingOrder() = runBlocking {
        val id1 = searchSettingDao.create(DbSearchSetting(text = "first"))
        val id2 = searchSettingDao.create(DbSearchSetting(text = "second"))
        val id3 = searchSettingDao.create(DbSearchSetting(text = "third"))
        val all = searchSettingDao.getSearchSettings().first()
        assertEquals(listOf(id3, id2, id1), all.map { it.uid })
    }

    @Test
    fun getSearchSettings_handlesEmptyDatabase() = runBlocking {
        assertEquals(emptyList<DbSearchSetting>(), searchSettingDao.getSearchSettings().first())
    }

    @Test
    fun get_returnsNullWhenSettingDoesNotExist() = runBlocking {
        assertNull(searchSettingDao.get(999L))
    }

    @Test
    fun getInGroup_returnsOnlyThatGroupsSettings() = runBlocking {
        searchSettingDao.create(DbSearchSetting(text = "mine", searchGroupId = 1L))
        searchSettingDao.create(DbSearchSetting(text = "theirs", searchGroupId = 2L))
        searchSettingDao.create(DbSearchSetting(text = "ungrouped", searchGroupId = null))

        val inGroup = searchSettingDao.getInGroup(1L).first()

        assertEquals(listOf("mine"), inGroup.map { it.text })
    }

    @Test
    fun getInGroup_returnsSettingsInDescendingOrder() = runBlocking {
        val id1 = searchSettingDao.create(DbSearchSetting(text = "first", searchGroupId = 1L))
        val id2 = searchSettingDao.create(DbSearchSetting(text = "second", searchGroupId = 1L))
        val id3 = searchSettingDao.create(DbSearchSetting(text = "third", searchGroupId = 1L))

        val inGroup = searchSettingDao.getInGroup(1L).first()

        assertEquals(listOf(id3, id2, id1), inGroup.map { it.uid })
    }

    @Test
    fun getInGroup_returnsEmptyWhenGroupHasNoSettings() = runBlocking {
        searchSettingDao.create(DbSearchSetting(text = "theirs", searchGroupId = 2L))

        assertEquals(emptyList<DbSearchSetting>(), searchSettingDao.getInGroup(1L).first())
    }

    @Test
    fun deleteByIds_removesOnlyTheGivenSettings() = runBlocking {
        val id1 = searchSettingDao.create(DbSearchSetting(text = "first"))
        val id2 = searchSettingDao.create(DbSearchSetting(text = "second"))
        val id3 = searchSettingDao.create(DbSearchSetting(text = "third"))

        searchSettingDao.delete(setOf(SearchSettingId(id1), SearchSettingId(id3)))

        assertEquals(listOf(id2), searchSettingDao.getSearchSettings().first().map { it.uid })
    }

    @Test
    fun deleteByIds_withAnEmptySetRemovesNothing() = runBlocking {
        searchSettingDao.create(DbSearchSetting(text = "keep me"))

        searchSettingDao.delete(emptySet())

        assertEquals(listOf("keep me"), searchSettingDao.getSearchSettings().first().map { it.text })
    }

    @Test
    fun deleteByIds_ignoresIdsThatDoNotExist() = runBlocking {
        val id = searchSettingDao.create(DbSearchSetting(text = "keep me"))

        searchSettingDao.delete(setOf(SearchSettingId(999L)))

        assertEquals(listOf(id), searchSettingDao.getSearchSettings().first().map { it.uid })
    }

    @Test
    fun deleteAll_removesEverySetting() = runBlocking {
        searchSettingDao.create(DbSearchSetting(text = "first"))
        searchSettingDao.create(DbSearchSetting(text = "second"))

        searchSettingDao.deleteAll()

        assertEquals(emptyList<DbSearchSetting>(), searchSettingDao.getSearchSettings().first())
    }

    @Test
    fun insertAll_insertsEverySettingWithItsGivenId() = runBlocking {
        searchSettingDao.insertAll(
            listOf(
                DbSearchSetting(uid = 5L, text = "five", caseSensitive = true, color = 0x555555),
                DbSearchSetting(uid = 6L, text = "six", searchGroupId = 3L),
            ),
        )

        val all = searchSettingDao.getSearchSettings().first()
        assertEquals(listOf(6L, 5L), all.map { it.uid })
        assertEquals(listOf("six", "five"), all.map { it.text })
        assertEquals(0x555555, all.last().color)
        assertEquals(3L, all.first().searchGroupId)
    }
}
