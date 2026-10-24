package com.ascolith.textseek.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.ui.TsTopDestination
import com.ascolith.textseek.feature.camerascreen.ui.navigation.CameraScreenRoute
import com.ascolith.textseek.feature.overview.ui.navigation.OverviewScreenRoute
import com.ascolith.textseek.feature.settings.ui.navigation.SettingsScreenRoute

internal val startDestination: TsTopDestination = TsTopDestination.Search

internal fun TsTopDestination.toNavKey(): NavKey =
    when (this) {
        TsTopDestination.Search -> CameraScreenRoute
        TsTopDestination.Overview -> OverviewScreenRoute
        TsTopDestination.Settings -> SettingsScreenRoute
    }

/** Null for a destination that is not top-level, such as one of the editor screens. */
internal fun NavKey.toTsTopDestinationOrNull(): TsTopDestination? =
    TsTopDestination.entries.firstOrNull { it.toNavKey() == this }
