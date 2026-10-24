package com.ascolith.textseek.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.core.ui.TsTopDestination
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.camerascreen.ui.navigation.CameraScreenRoute
import com.ascolith.textseek.feature.overview.ui.navigation.OverviewScreenRoute
import com.ascolith.textseek.feature.searchgroup.ui.navigation.SearchGroupScreenRoute
import com.ascolith.textseek.feature.searchsetting.ui.navigation.SearchSettingScreenRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchEntriesTest {

    private val navigator = RecordingNavigator()

    private val provider = entryProvider<NavKey> {
        searchEntries(navigator = navigator, scaffoldState = TsScaffoldState(), quickSearchLaunch = null)
    }

    @Test
    fun `search and manage are top destinations`() {
        assertEquals(barPosition(TsTopDestination.Search), provider(CameraScreenRoute).metadata)
        assertEquals(barPosition(TsTopDestination.Overview), provider(OverviewScreenRoute).metadata)
    }

    @Test
    fun `the search setting editor rises over its opener`() {
        assertEquals(riseAndFall, provider(SearchSettingScreenRoute()).metadata)
        assertEquals(riseAndFall, provider(SearchSettingScreenRoute(id = SearchSettingId(1))).metadata)
    }

    @Test
    fun `the search group editor rises over its opener`() {
        assertEquals(riseAndFall, provider(SearchGroupScreenRoute()).metadata)
        assertEquals(riseAndFall, provider(SearchGroupScreenRoute(id = SearchGroupId(1))).metadata)
    }

    @Test
    fun `adding a search setting opens a new one defaulted to its group`() {
        navigator.addSearchSetting(from = OverviewScreenRoute, groupId = SearchGroupId(7))

        assertEquals(
            listOf(OverviewScreenRoute to SearchSettingScreenRoute(groupIdDefault = SearchGroupId(7))),
            navigator.navigations,
        )
    }

    @Test
    fun `editing a search setting opens it`() {
        navigator.editSearchSetting(from = CameraScreenRoute, id = SearchSettingId(3))

        assertEquals(
            listOf(CameraScreenRoute to SearchSettingScreenRoute(id = SearchSettingId(3))),
            navigator.navigations,
        )
    }

    @Test
    fun `adding a search group opens a new one`() {
        navigator.addSearchGroup(from = OverviewScreenRoute)

        assertEquals(listOf(OverviewScreenRoute to SearchGroupScreenRoute()), navigator.navigations)
    }

    @Test
    fun `editing a search group opens it`() {
        navigator.editSearchGroup(from = CameraScreenRoute, id = SearchGroupId(5))

        assertEquals(
            listOf(CameraScreenRoute to SearchGroupScreenRoute(id = SearchGroupId(5))),
            navigator.navigations,
        )
    }
}
