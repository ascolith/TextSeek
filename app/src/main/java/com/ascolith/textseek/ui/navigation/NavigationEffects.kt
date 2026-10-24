package com.ascolith.textseek.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.ConfigurableScaffold
import com.ascolith.textseek.core.model.ScaffoldConfig
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.feature.camerascreen.ui.model.QuickSearchLaunch

// The scaffold's chrome belongs to whichever screen is on top
@Composable
internal fun ScaffoldOwnerEffect(scaffoldState: TsScaffoldState, currentRoute: NavKey) {
    SideEffect {
        scaffoldState.updateOwner(
            newOwner = currentRoute,
            newConfig = (currentRoute as? ConfigurableScaffold)?.getScaffoldConfig() ?: ScaffoldConfig(),
        )
    }
}

// A quick search launch can arrive while another screen is open (the
// camera screen only handles it while composed), so pop back to it.
// A recreated activity hands the same launch back, so the id is saved to pop once per
// launch rather than once per recreation, which would discard the restored back stack.
@Composable
internal fun QuickSearchLaunchEffect(quickSearchLaunch: QuickSearchLaunch?, navigator: MainNavigator) {
    var lastPoppedQuickSearchLaunchId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(quickSearchLaunch) {
        if (quickSearchLaunch != null && quickSearchLaunch.launchId != lastPoppedQuickSearchLaunchId) {
            lastPoppedQuickSearchLaunchId = quickSearchLaunch.launchId
            navigator.navigateToTopDestination(startDestination)
        }
    }
}
