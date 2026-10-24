package com.ascolith.textseek.ui.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.feature.debug.ui.DebugMenuScreen
import com.ascolith.textseek.feature.debug.ui.navigation.DebugMenuScreenRoute

// The release source set has a stub of this file, so :feature-debug is never on its classpath

internal val debugMenuRoute: NavKey? = DebugMenuScreenRoute

internal fun EntryProviderScope<NavKey>.debugMenuEntries(scaffoldState: TsScaffoldState) {
    editorEntry<DebugMenuScreenRoute> {
        DebugMenuScreen(scaffoldState = scaffoldState)
    }
}
