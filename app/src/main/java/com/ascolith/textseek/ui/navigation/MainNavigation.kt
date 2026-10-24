package com.ascolith.textseek.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.core.model.rememberTsScaffoldState
import com.ascolith.textseek.feature.camerascreen.ui.model.QuickSearchLaunch
import com.ascolith.textseek.feature.reportissue.ui.crashprompt.CrashPrompt
import com.ascolith.textseek.feature.reportissue.ui.navigation.ReportIssueScreenRoute

@Composable
fun MainNavigation(
    quickSearchLaunch: QuickSearchLaunch? = null,
) {
    val scaffoldState = rememberTsScaffoldState()
    val navigator = rememberMainNavigator()

    ScaffoldOwnerEffect(scaffoldState = scaffoldState, currentRoute = navigator.currentRoute)
    QuickSearchLaunchEffect(quickSearchLaunch = quickSearchLaunch, navigator = navigator)
    CrashPrompt(onReportIssue = { navigator.open(ReportIssueScreenRoute) })

    MainScaffold(
        scaffoldState = scaffoldState,
        currentTopDestination = navigator.currentRoute.toTsTopDestinationOrNull(),
        onBack = navigator::goBack,
        onTopDestinationSelected = navigator::navigateToTopDestination,
    ) { innerPadding ->
        MainNavDisplay(
            navigator = navigator,
            modifier = Modifier.padding(innerPadding),
            entryProvider = mainEntryProvider(
                navigator = navigator,
                scaffoldState = scaffoldState,
                quickSearchLaunch = quickSearchLaunch,
            ),
        )
    }
}

@Composable
internal fun MainNavDisplay(
    navigator: MainNavigator,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        modifier = modifier,
        backStack = navigator.routes,
        onBack = navigator::goBack,
        transitionSpec = { lateralSlide() },
        popTransitionSpec = { lateralSlide() },
        predictivePopTransitionSpec = { lateralSlide() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
    )
}

// Each feature area registers its own screens
private fun mainEntryProvider(
    navigator: EntryNavigator,
    scaffoldState: TsScaffoldState,
    quickSearchLaunch: QuickSearchLaunch?,
) = entryProvider {
    searchEntries(navigator = navigator, scaffoldState = scaffoldState, quickSearchLaunch = quickSearchLaunch)
    settingsEntries(navigator = navigator, scaffoldState = scaffoldState)
}
