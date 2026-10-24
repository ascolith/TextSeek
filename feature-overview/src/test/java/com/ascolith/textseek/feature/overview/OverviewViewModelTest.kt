package com.ascolith.textseek.feature.overview

import com.ascolith.textseek.core.data.searchgroup.ISearchGroupRepository
import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.overview.ui.OverviewViewModel
import com.ascolith.textseek.feature.overview.ui.model.ScreenIntent
import com.ascolith.textseek.feature.overview.ui.model.SideEffect
import com.ascolith.textseek.feature.overview.ui.model.TransientScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    private val groceries = SearchGroup(
        id = GROCERIES_ID,
        name = "Groceries",
        enabled = true,
        colorArgb = GROUP_COLOR_ARGB,
    )
    private val hardware = SearchGroup(
        id = HARDWARE_ID,
        name = "Hardware",
        enabled = false,
        colorArgb = SECOND_GROUP_COLOR_ARGB,
    )

    private val searchGroupsFlow = MutableStateFlow(listOf(groceries))
    private val searchSettingsFlow = MutableStateFlow<List<SearchSetting>>(emptyList())
    private val groupRepository = RecordingSearchGroupRepository(searchGroupsFlow)

    private fun buildViewModel() = OverviewViewModel(
        searchGroupRepository = groupRepository,
        searchSettingRepository = FakeSearchSettingRepository(searchSettingsFlow),
    )

    @Test
    fun `settings are grouped under the group they belong to`() = runTest {
        searchGroupsFlow.value = listOf(groceries, hardware)
        searchSettingsFlow.value = listOf(
            searchSetting(id = 10L, text = "milk", groupId = GROCERIES_ID),
            searchSetting(id = 11L, text = "eggs", groupId = GROCERIES_ID),
            searchSetting(id = 12L, text = "screws", groupId = HARDWARE_ID),
        )

        buildViewModel().test(this) {
            runOnCreate()
            val loaded = awaitState()

            assertEquals(listOf(GROCERIES_ID, HARDWARE_ID), loaded.overviewItems.keys.toList())
            assertEquals(
                listOf("milk", "eggs"),
                loaded.overviewItems.getValue(GROCERIES_ID).settings.values.map { it.text },
            )
            assertEquals(
                listOf("screws"),
                loaded.overviewItems.getValue(HARDWARE_ID).settings.values.map { it.text },
            )
            assertEquals(TransientScreen.None, loaded.transientScreen)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a group with no settings still gets a row`() = runTest {
        searchSettingsFlow.value = emptyList()

        buildViewModel().test(this) {
            runOnCreate()
            val loaded = awaitState()

            assertEquals(setOf(GROCERIES_ID), loaded.overviewItems.keys)
            assertTrue(loaded.overviewItems.getValue(GROCERIES_ID).settings.isEmpty())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `settings whose group is absent are not attached to any row`() = runTest {
        searchSettingsFlow.value = listOf(
            searchSetting(id = 10L, text = "milk", groupId = GROCERIES_ID),
            // No group at all, and a group that is not in the group flow.
            searchSetting(id = 11L, text = "orphan", groupId = null),
            searchSetting(id = 12L, text = "stale", groupId = SearchGroupId(999L)),
        )

        buildViewModel().test(this) {
            runOnCreate()
            val loaded = awaitState()

            assertEquals(setOf(GROCERIES_ID), loaded.overviewItems.keys)
            assertEquals(
                listOf("milk"),
                loaded.overviewItems.getValue(GROCERIES_ID).settings.values.map { it.text },
            )

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `toggling a group writes the inverse of its current enabled state`() = runTest {
        buildViewModel().test(this) {
            runOnCreate()
            awaitState() // Groups and settings loaded.

            // groceries is enabled, so the toggle must disable it.
            containerHost.handleIntent(ScreenIntent.ToggleSearchGroupEnabled(GROCERIES_ID))
            runCurrent()

            assertEquals(listOf(GROCERIES_ID to false), groupRepository.enabledWrites)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `toggling a group that is no longer shown is ignored rather than throwing`() = runTest {
        buildViewModel().test(this) {
            runOnCreate()
            awaitState() // Groups and settings loaded.

            containerHost.handleIntent(ScreenIntent.ToggleSearchGroupEnabled(SearchGroupId(999L)))
            runCurrent()

            assertTrue(groupRepository.enabledWrites.isEmpty())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `screen intents post the matching navigation side effects`() = runTest {
        buildViewModel().test(this) {
            runOnCreate()
            awaitState() // Groups and settings loaded.

            containerHost.handleIntent(ScreenIntent.SearchGroupAddClicked)
            assertEquals(SideEffect.NavigateToSearchGroupAdd, awaitSideEffect())

            containerHost.handleIntent(ScreenIntent.SearchGroupEditClicked(GROCERIES_ID))
            assertEquals(SideEffect.NavigateToSearchGroupEdit(GROCERIES_ID), awaitSideEffect())

            containerHost.handleIntent(ScreenIntent.SearchSettingAddClicked(GROCERIES_ID))
            assertEquals(SideEffect.NavigateToSearchSettingAdd(GROCERIES_ID), awaitSideEffect())

            containerHost.handleIntent(ScreenIntent.SearchSettingEditClicked(SearchSettingId(10L)))
            assertEquals(SideEffect.NavigateToSearchSettingEdit(SearchSettingId(10L)), awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    private fun searchSetting(
        id: Long,
        text: String,
        groupId: SearchGroupId?,
        enabled: Boolean = true,
        color: Int? = null,
    ) = SearchSetting(
        id = SearchSettingId(id),
        text = text,
        caseSensitive = false,
        color = color,
        enabled = enabled,
        searchGroupId = groupId,
    )

    private class RecordingSearchGroupRepository(
        override val searchGroups: Flow<List<SearchGroup>>,
    ) : ISearchGroupRepository {
        private val _enabledWrites = mutableListOf<Pair<SearchGroupId, Boolean>>()
        val enabledWrites: List<Pair<SearchGroupId, Boolean>> get() = _enabledWrites

        override suspend fun create(name: String, enabled: Boolean, colorArgb: Int): SearchGroupId =
            SearchGroupId(0L)

        override suspend fun get(id: SearchGroupId): SearchGroup? = null

        override suspend fun setGroupEnabled(id: SearchGroupId, enabled: Boolean) {
            _enabledWrites += id to enabled
        }

        override suspend fun update(searchGroup: SearchGroup) = Unit
        override suspend fun deleteGroupAndChildren(id: SearchGroupId) = Unit
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
        val GROCERIES_ID = SearchGroupId(1L)
        val HARDWARE_ID = SearchGroupId(2L)
        const val GROUP_COLOR_ARGB = 0xFFFF0000.toInt()
        const val SECOND_GROUP_COLOR_ARGB = 0xFF0000FF.toInt()
    }
}
