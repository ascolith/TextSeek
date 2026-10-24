package com.ascolith.textseek.ui.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.R as AppR
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.feature.acknowledgements.ui.AcknowledgementsScreen
import com.ascolith.textseek.feature.acknowledgements.ui.navigation.AcknowledgementsScreenRoute
import com.ascolith.textseek.feature.reportissue.ui.ReportIssueScreen
import com.ascolith.textseek.feature.reportissue.ui.navigation.ReportIssueScreenRoute
import com.ascolith.textseek.feature.settings.ui.SettingsScreen
import com.ascolith.textseek.feature.settings.ui.navigation.SettingsScreenRoute

internal fun EntryProviderScope<NavKey>.settingsEntries(
    navigator: EntryNavigator,
    scaffoldState: TsScaffoldState,
) {
    topDestinationEntry<SettingsScreenRoute> { route ->
        SettingsScreen(
            scaffoldState = scaffoldState,
            onReportIssueClicked = { navigator.navigate(route, ReportIssueScreenRoute) },
            onAcknowledgementsClicked = { navigator.navigate(route, AcknowledgementsScreenRoute) },
            onDebugMenuClicked = debugMenuRoute?.let { debugRoute -> { navigator.navigate(route, debugRoute) } },
        )
    }

    editorEntry<ReportIssueScreenRoute> {
        ReportIssueScreen(scaffoldState = scaffoldState)
    }

    editorEntry<AcknowledgementsScreenRoute> {
        AcknowledgementsScreen(librariesResId = AppR.raw.aboutlibraries)
    }

    debugMenuEntries(scaffoldState = scaffoldState)
}
