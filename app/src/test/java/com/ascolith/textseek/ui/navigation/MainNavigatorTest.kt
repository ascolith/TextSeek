package com.ascolith.textseek.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.ui.TsTopDestination
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.camerascreen.ui.navigation.CameraScreenRoute
import com.ascolith.textseek.feature.overview.ui.navigation.OverviewScreenRoute
import com.ascolith.textseek.feature.reportissue.ui.navigation.ReportIssueScreenRoute
import com.ascolith.textseek.feature.searchgroup.ui.navigation.SearchGroupScreenRoute
import com.ascolith.textseek.feature.searchsetting.ui.navigation.SearchSettingScreenRoute
import com.ascolith.textseek.feature.settings.ui.navigation.SettingsScreenRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigatorTest {

    private val editor = SearchSettingScreenRoute(id = SearchSettingId(1))

    private fun navigatorWith(vararg routes: NavKey) = MainNavigator(NavBackStack(*routes))

    @Test
    fun `routes lists the back stack from the bottom up`() {
        val navigator = navigatorWith(CameraScreenRoute, OverviewScreenRoute, editor)

        assertEquals(listOf(CameraScreenRoute, OverviewScreenRoute, editor), navigator.routes)
    }

    @Test
    fun `the current route is the one on top`() {
        val navigator = navigatorWith(CameraScreenRoute, OverviewScreenRoute)

        assertEquals(OverviewScreenRoute, navigator.currentRoute)
    }

    @Test
    fun `open pushes a route`() {
        val navigator = navigatorWith(CameraScreenRoute)

        navigator.open(ReportIssueScreenRoute)

        assertEquals(listOf(CameraScreenRoute, ReportIssueScreenRoute), navigator.routes)
    }

    @Test
    fun `the screen on top can open another`() {
        val navigator = navigatorWith(CameraScreenRoute, OverviewScreenRoute)

        navigator.navigate(from = OverviewScreenRoute, to = editor)

        assertEquals(listOf(CameraScreenRoute, OverviewScreenRoute, editor), navigator.routes)
    }

    @Test
    fun `a screen that is no longer on top cannot open another`() {
        val navigator = navigatorWith(CameraScreenRoute, OverviewScreenRoute, editor)

        // e.g. a second tap landing after the first already opened the editor
        navigator.navigate(from = OverviewScreenRoute, to = SearchGroupScreenRoute())

        assertEquals(listOf(CameraScreenRoute, OverviewScreenRoute, editor), navigator.routes)
    }

    @Test
    fun `the screen on top can close itself`() {
        val navigator = navigatorWith(CameraScreenRoute, OverviewScreenRoute, editor)

        navigator.close(editor)

        assertEquals(listOf(CameraScreenRoute, OverviewScreenRoute), navigator.routes)
    }

    @Test
    fun `a screen that is no longer on top cannot close`() {
        val navigator = navigatorWith(CameraScreenRoute, OverviewScreenRoute, editor)

        navigator.close(OverviewScreenRoute)

        assertEquals(listOf(CameraScreenRoute, OverviewScreenRoute, editor), navigator.routes)
    }

    @Test
    fun `the start screen cannot close itself`() {
        val navigator = navigatorWith(CameraScreenRoute)

        navigator.close(CameraScreenRoute)

        assertEquals(listOf(CameraScreenRoute), navigator.routes)
    }

    @Test
    fun `going back pops the top route`() {
        val navigator = navigatorWith(CameraScreenRoute, OverviewScreenRoute, editor)

        navigator.goBack()

        assertEquals(listOf(CameraScreenRoute, OverviewScreenRoute), navigator.routes)
    }

    @Test
    fun `going back never empties the back stack`() {
        val navigator = navigatorWith(CameraScreenRoute)

        navigator.goBack()

        assertEquals(listOf(CameraScreenRoute), navigator.routes)
    }

    @Test
    fun `navigating to the start destination pops everything above it`() {
        val navigator = navigatorWith(CameraScreenRoute, SettingsScreenRoute, ReportIssueScreenRoute)

        navigator.navigateToTopDestination(TsTopDestination.Search)

        assertEquals(listOf(CameraScreenRoute), navigator.routes)
    }

    @Test
    fun `navigating to the start destination from it changes nothing`() {
        val navigator = navigatorWith(CameraScreenRoute)

        navigator.navigateToTopDestination(TsTopDestination.Search)

        assertEquals(listOf(CameraScreenRoute), navigator.routes)
    }

    @Test
    fun `navigating to another top destination stacks it on the start destination`() {
        val navigator = navigatorWith(CameraScreenRoute)

        navigator.navigateToTopDestination(TsTopDestination.Overview)

        assertEquals(listOf(CameraScreenRoute, OverviewScreenRoute), navigator.routes)
    }

    @Test
    fun `navigating to another top destination replaces everything above the start destination`() {
        val navigator = navigatorWith(CameraScreenRoute, OverviewScreenRoute, editor)

        navigator.navigateToTopDestination(TsTopDestination.Settings)

        assertEquals(listOf(CameraScreenRoute, SettingsScreenRoute), navigator.routes)
    }

    @Test
    fun `navigating to another top destination from an editor over the start destination replaces the editor`() {
        val navigator = navigatorWith(CameraScreenRoute, editor)

        navigator.navigateToTopDestination(TsTopDestination.Overview)

        assertEquals(listOf(CameraScreenRoute, OverviewScreenRoute), navigator.routes)
    }
}
