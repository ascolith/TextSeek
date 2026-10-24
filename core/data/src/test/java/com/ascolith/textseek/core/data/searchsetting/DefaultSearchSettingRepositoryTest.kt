package com.ascolith.textseek.core.data.searchsetting

import com.ascolith.textseek.core.data.FakeSearchSettingDao
import com.ascolith.textseek.core.database.entity.DbSearchSetting
import com.ascolith.textseek.core.testing.MainDispatcherRule
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DefaultSearchSettingRepositoryTest {

    private lateinit var repository: DefaultSearchSettingRepository
    private lateinit var fakeDao: FakeSearchSettingDao

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setup() {
        fakeDao = FakeSearchSettingDao()
        repository = DefaultSearchSettingRepository(fakeDao, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun `searchSettings flow returns mapped domain models in descending order`() = runTest {
        // Given
        fakeDao.create(DbSearchSetting(text = "first", caseSensitive = false, color = 0xFF0000))
        fakeDao.create(DbSearchSetting(text = "second", caseSensitive = true, color = 0x00FF00))
        fakeDao.create(DbSearchSetting(text = "third", caseSensitive = false, color = null))

        // When
        val result = repository.searchSettings.first()

        // Then
        assertEquals(3, result.size)
        // Should be ordered by uid DESC (newest first)
        assertEquals("third", result[0].text)
        assertEquals(false, result[0].caseSensitive)
        assertEquals(null, result[0].color)

        assertEquals("second", result[1].text)
        assertEquals(true, result[1].caseSensitive)
        assertEquals(0x00FF00, result[1].color)

        assertEquals("first", result[2].text)
        assertEquals(false, result[2].caseSensitive)
        assertEquals(0xFF0000, result[2].color)
    }

    @Test
    fun `get returns mapped domain model when setting exists`() = runTest {
        // Given
        val insertedId = fakeDao.create(
            DbSearchSetting(
                text = "test search",
                caseSensitive = true,
                color = 0x123456,
                enabled = false,
                searchGroupId = 5L,
            )
        )

        // When
        val result = repository.get(SearchSettingId(insertedId))

        // Then
        assertEquals(SearchSettingId(insertedId), result?.id)
        assertEquals("test search", result?.text)
        assertEquals(true, result?.caseSensitive)
        assertEquals(0x123456, result?.color)
        assertEquals(false, result?.enabled)
        assertEquals(SearchGroupId(5L), result?.searchGroupId)
    }

    @Test
    fun `get returns null when setting does not exist`() = runTest {
        // When
        val result = repository.get(SearchSettingId(999L))

        // Then
        assertNull(result)
    }

    @Test
    fun `getInGroup returns only settings in the group`() = runTest {
        // Given
        fakeDao.create(DbSearchSetting(text = "in group", searchGroupId = 1L))
        fakeDao.create(DbSearchSetting(text = "other group", searchGroupId = 2L))

        // When
        val result = repository.getInGroup(SearchGroupId(1L)).first()

        // Then
        assertEquals(1, result.size)
        assertEquals("in group", result[0].text)
    }

    @Test
    fun `create persists setting and returns its id`() = runTest {
        // When
        val createdId = repository.create(
            text = "created",
            caseSensitive = true,
            color = 0x654321,
            enabled = true,
            searchGroupId = SearchGroupId(1L),
        )

        // Then
        val created = repository.get(SearchSettingId(createdId))
        assertEquals("created", created?.text)
        assertEquals(true, created?.caseSensitive)
        assertEquals(0x654321, created?.color)
        assertEquals(true, created?.enabled)
        assertEquals(SearchGroupId(1L), created?.searchGroupId)
    }

    @Test
    fun `update with SearchSetting updates existing setting`() = runTest {
        // Given
        fakeDao.create(DbSearchSetting(uid = 1L, text = "original text", caseSensitive = false, color = 0xFF0000))

        val updatedSetting = SearchSetting(
            id = SearchSettingId(1L),
            text = "updated text",
            caseSensitive = true,
            color = 0x00FF00,
            enabled = false,
            searchGroupId = null,
        )

        // When
        repository.update(updatedSetting)

        // Then
        val result = repository.get(SearchSettingId(1L))
        assertEquals(updatedSetting, result)
    }

    @Test
    fun `update with id and text updates only text field`() = runTest {
        // Given
        fakeDao.create(DbSearchSetting(uid = 1L, text = "original text", caseSensitive = true, color = 0xFF0000))

        // When
        repository.update(SearchSettingId(1L), "new text")

        // Then
        val result = repository.get(SearchSettingId(1L))
        assertEquals("new text", result?.text)
        assertEquals(true, result?.caseSensitive) // Should remain unchanged
        assertEquals(0xFF0000, result?.color) // Should remain unchanged
    }

    @Test
    fun `delete removes single setting`() = runTest {
        // Given
        fakeDao.create(DbSearchSetting(uid = 1L, text = "to delete"))

        // Verify it exists
        assertEquals("to delete", repository.get(SearchSettingId(1L))?.text)

        // When
        repository.delete(SearchSettingId(1L))

        // Then
        assertNull(repository.get(SearchSettingId(1L)))
    }

    @Test
    fun `delete removes multiple settings`() = runTest {
        // Given
        fakeDao.create(DbSearchSetting(uid = 1L, text = "first"))
        fakeDao.create(DbSearchSetting(uid = 2L, text = "second"))
        fakeDao.create(DbSearchSetting(uid = 3L, text = "third"))

        // Verify all exist
        assertEquals(3, repository.searchSettings.first().size)

        // When
        repository.delete(setOf(SearchSettingId(1L), SearchSettingId(2L)))

        // Then
        val remaining = repository.searchSettings.first()
        assertEquals(1, remaining.size)
        assertEquals("third", remaining[0].text)
    }

    @Test
    fun `searchSettings flow handles empty database`() = runTest {
        // When
        val result = repository.searchSettings.first()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `update with non-existent id does not throw exception`() = runTest {
        // When & Then - should not throw exception
        repository.update(SearchSettingId(999L), "new text")
    }

    @Test
    fun `delete with non-existent id does not throw exception`() = runTest {
        // When & Then - should not throw exception
        repository.delete(SearchSettingId(999L))
    }
}
