package com.ascolith.textseek.feature.debug

import com.ascolith.textseek.feature.debug.ui.SampleDataSeeder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleDataSeederTest {

    private val searchGroupRepository = FakeSearchGroupRepository()
    private val searchSettingRepository = FakeSearchSettingRepository()

    private val seeder = SampleDataSeeder(
        searchGroupRepository = searchGroupRepository,
        searchSettingRepository = searchSettingRepository,
    )

    @Test
    fun `seeding creates a group whose settings belong to it`() = runTest {
        seeder.seed()

        val group = searchGroupRepository.searchGroups.value.single()
        val grouped = searchSettingRepository.searchSettings.value.filter { it.searchGroupId == group.id }
        assertTrue("expected settings in the sample group", grouped.isNotEmpty())
    }

    @Test
    fun `seeding creates ungrouped settings covering disabled and case sensitive rows`() = runTest {
        seeder.seed()

        val ungrouped = searchSettingRepository.searchSettings.value.filter { it.searchGroupId == null }
        assertTrue("expected a disabled setting", ungrouped.any { !it.enabled })
        assertTrue("expected a case-sensitive setting", ungrouped.any { it.caseSensitive })
    }

    @Test
    fun `seeding twice adds a second copy rather than failing`() = runTest {
        seeder.seed()
        val once = searchSettingRepository.searchSettings.value.size

        seeder.seed()

        assertEquals(2, searchGroupRepository.searchGroups.value.size)
        assertEquals(once * 2, searchSettingRepository.searchSettings.value.size)
    }
}
