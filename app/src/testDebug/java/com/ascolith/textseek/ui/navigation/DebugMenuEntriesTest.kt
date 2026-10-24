package com.ascolith.textseek.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.feature.debug.ui.navigation.DebugMenuScreenRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugMenuEntriesTest {

    @Test
    fun `debug builds offer the debug menu`() {
        assertEquals(DebugMenuScreenRoute, debugMenuRoute)
    }

    @Test
    fun `the debug menu rises over settings`() {
        val provider = entryProvider<NavKey> {
            settingsEntries(navigator = RecordingNavigator(), scaffoldState = TsScaffoldState())
        }

        assertEquals(riseAndFall, provider(DebugMenuScreenRoute).metadata)
    }
}
