package com.ascolith.textseek.feature.searchsetting

import com.ascolith.textseek.core.data.searchgroup.ISearchGroupRepository
import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.searchsetting.ui.SearchSettingViewModel
import com.ascolith.textseek.feature.searchsetting.ui.model.ScreenIntent
import com.ascolith.textseek.feature.searchsetting.ui.model.TransientScreen
import com.ascolith.textseek.feature.searchsetting.ui.model.TsDialog
import com.ascolith.textseek.feature.searchsetting.ui.navigation.SearchSettingScreenRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchSettingViewModelTest {

    private val searchGroup = SearchGroup(
        id = GROUP_ID,
        name = "Groceries",
        enabled = true,
        colorArgb = GROUP_COLOR_ARGB,
    )
    private val searchGroupsFlow = MutableStateFlow(listOf(searchGroup))

    private fun buildViewModel(
        route: SearchSettingScreenRoute,
        storedSetting: SearchSetting? = null,
    ) = SearchSettingViewModel(
        route = route,
        searchSettingRepository = FakeSearchSettingRepository(searchGroupsFlow, storedSetting),
        searchGroupRepository = FakeSearchGroupRepository(searchGroupsFlow, searchGroup),
    )

    @Test
    fun `opening a setting that no longer exists reports NotFound instead of throwing`() = runTest {
        // The row is gone: deleting a group cascades to its settings, and this screen can
        // be restored from a back stack that outlived it.
        buildViewModel(
            route = SearchSettingScreenRoute(id = SETTING_ID),
            storedSetting = null,
        ).test(this) {
            runOnCreate()
            val state = awaitState()

            assertEquals(TransientScreen.NotFound, state.transientScreen)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `opening an existing setting loads it`() = runTest {
        val stored = SearchSetting(
            id = SETTING_ID,
            text = "milk",
            caseSensitive = true,
            color = null,
            enabled = true,
            searchGroupId = GROUP_ID,
        )

        buildViewModel(
            route = SearchSettingScreenRoute(id = SETTING_ID),
            storedSetting = stored,
        ).test(this) {
            runOnCreate()
            val state = awaitState()

            assertEquals(TransientScreen.None, state.transientScreen)
            assertEquals("milk", state.text)
            assertEquals(true, state.caseSensitive)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `opening the screen with no id starts a blank setting`() = runTest {
        buildViewModel(route = SearchSettingScreenRoute()).test(this) {
            runOnCreate()
            val state = awaitState()

            assertEquals(TransientScreen.None, state.transientScreen)
            assertEquals("", state.text)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `confirming the color picker without choosing leaves the color unchanged`() = runTest {
        val stored = storedSetting(color = SETTING_COLOR_ARGB)

        buildViewModel(route = SearchSettingScreenRoute(id = SETTING_ID), storedSetting = stored).test(this) {
            runOnCreate()
            awaitState() // Setting loaded.

            // The picker only reports a color once dragged, so confirming straight away
            // arrives with nothing selected.
            containerHost.handleIntent(ScreenIntent.ShowDialog(TsDialog.ColorPicker()))
            awaitState()
            containerHost.handleIntent(ScreenIntent.SaveSearchSettingColor)
            val saved = awaitState()

            assertEquals(SETTING_COLOR_ARGB, saved.selectedColor.settingColorArgb)
            assertEquals(TsDialog.None, saved.dialog)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `confirming without choosing keeps an inheriting setting inheriting`() = runTest {
        val stored = storedSetting(color = null)

        buildViewModel(route = SearchSettingScreenRoute(id = SETTING_ID), storedSetting = stored).test(this) {
            runOnCreate()
            awaitState() // Setting loaded.

            containerHost.handleIntent(ScreenIntent.ShowDialog(TsDialog.ColorPicker()))
            awaitState()
            containerHost.handleIntent(ScreenIntent.SaveSearchSettingColor)
            val saved = awaitState()

            // No color of its own, so it still falls back to the group's - and the group
            // color has to survive, or require() has nothing left to return.
            assertNull(saved.selectedColor.settingColorArgb)
            assertEquals(GROUP_COLOR_ARGB, saved.selectedColor.groupColorArgb)
            assertEquals(GROUP_COLOR_ARGB, saved.selectedColor.require())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `confirming the color picker after choosing applies the chosen color`() = runTest {
        val stored = storedSetting(color = null)

        buildViewModel(route = SearchSettingScreenRoute(id = SETTING_ID), storedSetting = stored).test(this) {
            runOnCreate()
            awaitState() // Setting loaded.

            containerHost.handleIntent(ScreenIntent.ShowDialog(TsDialog.ColorPicker()))
            awaitState()
            containerHost.handleIntent(ScreenIntent.UpdateColorPickerSelectedColor(SETTING_COLOR_ARGB))
            awaitState()
            containerHost.handleIntent(ScreenIntent.SaveSearchSettingColor)
            val saved = awaitState()

            assertEquals(SETTING_COLOR_ARGB, saved.selectedColor.settingColorArgb)
            assertEquals(GROUP_COLOR_ARGB, saved.selectedColor.groupColorArgb)
            assertEquals(TsDialog.None, saved.dialog)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `using the group color clears an explicit color so the setting inherits again`() = runTest {
        val stored = storedSetting(color = SETTING_COLOR_ARGB)

        buildViewModel(route = SearchSettingScreenRoute(id = SETTING_ID), storedSetting = stored).test(this) {
            runOnCreate()
            awaitState() // Setting loaded.

            containerHost.handleIntent(ScreenIntent.ShowDialog(TsDialog.ColorPicker()))
            awaitState()
            containerHost.handleIntent(ScreenIntent.UseGroupColor)
            val reset = awaitState()

            // Null is what makes the inheritance live: save stores null and every consumer
            // resolves against the group at read time.
            assertNull(reset.selectedColor.settingColorArgb)
            assertEquals(GROUP_COLOR_ARGB, reset.selectedColor.groupColorArgb)
            assertEquals(GROUP_COLOR_ARGB, reset.selectedColor.require())
            assertEquals(TsDialog.None, reset.dialog)

            cancelAndIgnoreRemainingItems()
        }
    }

    private fun storedSetting(color: Int?) = SearchSetting(
        id = SETTING_ID,
        text = "milk",
        caseSensitive = false,
        color = color,
        enabled = true,
        searchGroupId = GROUP_ID,
    )

    private class FakeSearchSettingRepository(
        @Suppress("unused") private val groupsFlow: Flow<List<SearchGroup>>,
        private val storedSetting: SearchSetting?,
    ) : ISearchSettingRepository {
        override val searchSettings: Flow<List<SearchSetting>> =
            MutableStateFlow(listOfNotNull(storedSetting))

        override suspend fun create(
            text: String,
            caseSensitive: Boolean,
            color: Int?,
            enabled: Boolean,
            searchGroupId: SearchGroupId?,
        ): Long = 0L

        override suspend fun get(id: SearchSettingId): SearchSetting? =
            storedSetting?.takeIf { it.id == id }

        override suspend fun getInGroup(groupId: SearchGroupId): Flow<List<SearchSetting>> = searchSettings
        override suspend fun update(searchSetting: SearchSetting) = Unit
        override suspend fun update(id: SearchSettingId, text: String) = Unit
        override suspend fun delete(ids: Set<SearchSettingId>) = Unit
        override suspend fun delete(id: SearchSettingId) = Unit
    }

    private class FakeSearchGroupRepository(
        override val searchGroups: Flow<List<SearchGroup>>,
        private val searchGroup: SearchGroup,
    ) : ISearchGroupRepository {
        override suspend fun create(name: String, enabled: Boolean, colorArgb: Int): SearchGroupId =
            searchGroup.id

        override suspend fun get(id: SearchGroupId): SearchGroup? = searchGroup.takeIf { it.id == id }
        override suspend fun setGroupEnabled(id: SearchGroupId, enabled: Boolean) = Unit
        override suspend fun update(searchGroup: SearchGroup) = Unit
        override suspend fun deleteGroupAndChildren(id: SearchGroupId) = Unit
    }

    private companion object {
        val GROUP_ID = SearchGroupId(1L)
        val SETTING_ID = SearchSettingId(10L)
        const val GROUP_COLOR_ARGB = 0xFFFF0000.toInt()
        const val SETTING_COLOR_ARGB = 0xFF00FF00.toInt()
    }
}
