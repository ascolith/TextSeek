package com.ascolith.textseek.ui.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.model.TsScaffoldState

// Release builds have no debug menu; the debug source set supplies the real one

internal val debugMenuRoute: NavKey? = null

@Suppress("UnusedParameter", "UnusedReceiverParameter")
internal fun EntryProviderScope<NavKey>.debugMenuEntries(scaffoldState: TsScaffoldState) = Unit
