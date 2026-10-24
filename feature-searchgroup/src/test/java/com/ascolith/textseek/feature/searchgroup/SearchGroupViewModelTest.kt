package com.ascolith.textseek.feature.searchgroup

import com.ascolith.textseek.core.data.searchgroup.ISearchGroupRepository
import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.searchgroup.ui.SearchGroupViewModel
import com.ascolith.textseek.feature.searchgroup.ui.model.ScreenIntent
import com.ascolith.textseek.feature.searchgroup.ui.model.TransientScreen
import com.ascolith.textseek.feature.searchgroup.ui.navigation.SearchGroupScreenRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchGroupViewModelTest {

    private val searchSettingsFlow = MutableStateFlow<List<SearchSetting>>(emptyList())

    private var groupRepository = FakeSearchGroupRepository(null)

    private fun buildViewModel(
        route: SearchGroupScreenRoute,
        storedGroup: SearchGroup? = null,
    ): SearchGroupViewModel {
        groupRepository = FakeSearchGroupRepository(storedGroup)
        return SearchGroupViewModel(
            route = route,
            searchGroupRepository = groupRepository,
            searchSettingRepository = FakeSearchSettingRepository(searchSettingsFlow),
        )
    }

    @Test
    fun `opening a group that no longer exists reports NotFound instead of throwing`() = runTest {
        buildViewModel(
            route = SearchGroupScreenRoute(id = GROUP_ID),
            storedGroup = null,
        ).test(this) {
            runOnCreate()
            val state = awaitState()

            assertEquals(TransientScreen.NotFound, state.transientScreen)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `opening an existing group loads it`() = runTest {
        val stored = SearchGroup(
            id = GROUP_ID,
            name = "Groceries",
            enabled = false,
            colorArgb = GROUP_COLOR_ARGB,
        )

        buildViewModel(
            route = SearchGroupScreenRoute(id = GROUP_ID),
            storedGroup = stored,
        ).test(this) {
            runOnCreate()
            val state = awaitState()

            assertEquals(TransientScreen.None, state.transientScreen)
            assertEquals("Groceries", state.name)
            assertEquals(false, state.enabled)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `saving a new group stores the name trimmed`() = runTest {
        buildViewModel(route = SearchGroupScreenRoute()).test(this) {
            runOnCreate()
            awaitState()

            containerHost.handleIntent(ScreenIntent.UpdateSearchGroupName("  Groceries  "))
            awaitState()
            containerHost.handleIntent(ScreenIntent.Save)
            runCurrent()

            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf("Groceries"), groupRepository.created)
    }

    @Test
    fun `saving an existing group stores the name trimmed`() = runTest {
        val stored = SearchGroup(
            id = GROUP_ID,
            name = "Groceries",
            enabled = true,
            colorArgb = GROUP_COLOR_ARGB,
        )

        buildViewModel(route = SearchGroupScreenRoute(id = GROUP_ID), storedGroup = stored).test(this) {
            runOnCreate()
            awaitState()

            containerHost.handleIntent(ScreenIntent.UpdateSearchGroupName("  Hardware  "))
            awaitState()
            containerHost.handleIntent(ScreenIntent.Save)
            runCurrent()

            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf("Hardware"), groupRepository.updated.map { it.name })
    }

    @Test
    fun `settings belonging to the group are observed into state`() = runTest {
        val stored = SearchGroup(
            id = GROUP_ID,
            name = "Groceries",
            enabled = true,
            colorArgb = GROUP_COLOR_ARGB,
        )
        searchSettingsFlow.value = listOf(
            SearchSetting(
                id = SearchSettingId(10L),
                text = "milk",
                caseSensitive = false,
                color = null,
                enabled = true,
                searchGroupId = GROUP_ID,
            )
        )

        buildViewModel(route = SearchGroupScreenRoute(id = GROUP_ID), storedGroup = stored).test(this) {
            runOnCreate()
            awaitState() // Group loaded.
            val withSettings = awaitState() // Settings observed.

            assertEquals(listOf("milk"), withSettings.searchSettings.values.map { it.text })

            cancelAndIgnoreRemainingItems()
        }
    }

    private class FakeSearchGroupRepository(
        private val storedGroup: SearchGroup?,
    ) : ISearchGroupRepository {
        val created = mutableListOf<String>()
        val updated = mutableListOf<SearchGroup>()
        val deleted = mutableListOf<SearchGroupId>()

        override val searchGroups: Flow<List<SearchGroup>> =
            MutableStateFlow(listOfNotNull(storedGroup))

        override suspend fun create(name: String, enabled: Boolean, colorArgb: Int): SearchGroupId {
            created += name
            return SearchGroupId(0L)
        }

        override suspend fun get(id: SearchGroupId): SearchGroup? =
            storedGroup?.takeIf { it.id == id }

        override suspend fun setGroupEnabled(id: SearchGroupId, enabled: Boolean) = Unit

        override suspend fun update(searchGroup: SearchGroup) {
            updated += searchGroup
        }

        override suspend fun deleteGroupAndChildren(id: SearchGroupId) {
            deleted += id
        }
    }

    private class FakeSearchSettingRepository(
        override val searchSettings: Flow<List<SearchSetting>>,
    ) : ISearchSettingRepository {
        override suspend fun create(
            text: String,
            caseSensitive: Boolean,
            color: Int?,
            enabled: Boolean,
            searchGroupId: SearchGroupId?,
        ): Long = 0L

        override suspend fun get(id: SearchSettingId): SearchSetting? = null
        override suspend fun getInGroup(groupId: SearchGroupId): Flow<List<SearchSetting>> = searchSettings
        override suspend fun update(searchSetting: SearchSetting) = Unit
        override suspend fun update(id: SearchSettingId, text: String) = Unit
        override suspend fun delete(ids: Set<SearchSettingId>) = Unit
        override suspend fun delete(id: SearchSettingId) = Unit
    }

    private companion object {
        val GROUP_ID = SearchGroupId(1L)
        const val GROUP_COLOR_ARGB = 0xFFFF0000.toInt()
    }
}
