package com.ascolith.textseek.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.core.ui.TsTopDestination
import com.ascolith.textseek.feature.acknowledgements.ui.navigation.AcknowledgementsScreenRoute
import com.ascolith.textseek.feature.reportissue.ui.navigation.ReportIssueScreenRoute
import com.ascolith.textseek.feature.settings.ui.navigation.SettingsScreenRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsEntriesTest {

    private val provider = entryProvider<NavKey> {
        settingsEntries(navigator = RecordingNavigator(), scaffoldState = TsScaffoldState())
    }

    @Test
    fun `settings is a top destination`() {
        assertEquals(barPosition(TsTopDestination.Settings), provider(SettingsScreenRoute).metadata)
    }

    @Test
    fun `the issue reporter rises over settings`() {
        assertEquals(riseAndFall, provider(ReportIssueScreenRoute).metadata)
    }

    @Test
    fun `acknowledgements rise over settings`() {
        assertEquals(riseAndFall, provider(AcknowledgementsScreenRoute).metadata)
    }
}
