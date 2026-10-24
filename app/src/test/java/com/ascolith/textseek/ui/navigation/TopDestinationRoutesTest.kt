package com.ascolith.textseek.ui.navigation

import com.ascolith.textseek.core.ui.TsTopDestination
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.acknowledgements.ui.navigation.AcknowledgementsScreenRoute
import com.ascolith.textseek.feature.camerascreen.ui.navigation.CameraScreenRoute
import com.ascolith.textseek.feature.overview.ui.navigation.OverviewScreenRoute
import com.ascolith.textseek.feature.reportissue.ui.navigation.ReportIssueScreenRoute
import com.ascolith.textseek.feature.searchgroup.ui.navigation.SearchGroupScreenRoute
import com.ascolith.textseek.feature.searchsetting.ui.navigation.SearchSettingScreenRoute
import com.ascolith.textseek.feature.settings.ui.navigation.SettingsScreenRoute
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopDestinationRoutesTest {

    @Test
    fun `the app starts on search`() {
        assertEquals(TsTopDestination.Search, startDestination)
    }

    @Test
    fun `each top destination opens its screen`() {
        assertEquals(CameraScreenRoute, TsTopDestination.Search.toNavKey())
        assertEquals(OverviewScreenRoute, TsTopDestination.Overview.toNavKey())
        assertEquals(SettingsScreenRoute, TsTopDestination.Settings.toNavKey())
    }

    @Test
    fun `each top destination's screen maps back to it`() {
        TsTopDestination.entries.forEach { destination ->
            assertEquals(destination, destination.toNavKey().toTsTopDestinationOrNull())
        }
    }

    @Test
    fun `a restored top destination's screen still maps back to it`() {
        assertEquals(TsTopDestination.Search, restore(CameraScreenRoute).toTsTopDestinationOrNull())
        assertEquals(TsTopDestination.Overview, restore(OverviewScreenRoute).toTsTopDestinationOrNull())
        assertEquals(TsTopDestination.Settings, restore(SettingsScreenRoute).toTsTopDestinationOrNull())
    }

    @Test
    fun `screens off the bottom bar are not top destinations`() {
        listOf(
            SearchSettingScreenRoute(),
            SearchSettingScreenRoute(id = SearchSettingId(1)),
            SearchGroupScreenRoute(),
            SearchGroupScreenRoute(id = SearchGroupId(1)),
            ReportIssueScreenRoute,
            AcknowledgementsScreenRoute,
        ).forEach { route ->
            assertNull(route.toTsTopDestinationOrNull())
        }
    }

    // Round-trips a route the way a saved back stack does
    private inline fun <reified T> restore(route: T): T =
        Json.decodeFromString<T>(Json.encodeToString(route))
}
