package com.ascolith.textseek.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.ascolith.textseek.core.ui.TsTopDestination
import com.ascolith.textseek.feature.camerascreen.ui.navigation.CameraScreenRoute
import com.ascolith.textseek.feature.overview.ui.navigation.OverviewScreenRoute
import com.ascolith.textseek.feature.reportissue.ui.navigation.ReportIssueScreenRoute
import com.ascolith.textseek.feature.settings.ui.navigation.SettingsScreenRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class NavEntriesTest {

    private val provider = entryProvider<NavKey> {
        topDestinationEntry<CameraScreenRoute> {}
        topDestinationEntry<OverviewScreenRoute> {}
        topDestinationEntry<SettingsScreenRoute> {}
        editorEntry<ReportIssueScreenRoute> {}
    }

    @Test
    fun `a top destination entry carries its position on the bar`() {
        TsTopDestination.entries.forEach { destination ->
            assertEquals(barPosition(destination), provider(destination.toNavKey()).metadata)
        }
    }

    @Test
    fun `top destinations have distinct bar positions`() {
        val positions = TsTopDestination.entries.map { barPosition(it) }

        assertEquals(positions.size, positions.toSet().size)
    }

    @Test(expected = IllegalStateException::class)
    fun `a top destination entry for a screen off the bar fails`() {
        val misregistered = entryProvider<NavKey> { topDestinationEntry<ReportIssueScreenRoute> {} }

        misregistered(ReportIssueScreenRoute)
    }

    @Test
    fun `an editor entry rises and falls`() {
        assertEquals(riseAndFall, provider(ReportIssueScreenRoute).metadata)
    }

    @Test
    fun `rise and fall sets the push, pop and predictive pop transitions`() {
        val expectedKeys = (
            NavDisplay.transitionSpec { error("unused") } +
                NavDisplay.popTransitionSpec { error("unused") } +
                NavDisplay.predictivePopTransitionSpec { error("unused") }
            ).keys

        assertEquals(expectedKeys, riseAndFall.keys)
    }
}
