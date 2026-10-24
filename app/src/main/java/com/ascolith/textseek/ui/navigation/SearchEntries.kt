package com.ascolith.textseek.ui.navigation

import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.camerascreen.ui.CameraScreen
import com.ascolith.textseek.feature.camerascreen.ui.model.QuickSearchLaunch
import com.ascolith.textseek.feature.camerascreen.ui.navigation.CameraScreenRoute
import com.ascolith.textseek.feature.overview.ui.OverviewScreen
import com.ascolith.textseek.feature.overview.ui.navigation.OverviewScreenRoute
import com.ascolith.textseek.feature.searchgroup.ui.SearchGroupScreen
import com.ascolith.textseek.feature.searchgroup.ui.navigation.SearchGroupScreenRoute
import com.ascolith.textseek.feature.searchsetting.ui.SearchSettingScreen
import com.ascolith.textseek.feature.searchsetting.ui.navigation.SearchSettingScreenRoute

internal fun EntryProviderScope<NavKey>.searchEntries(
    navigator: EntryNavigator,
    scaffoldState: TsScaffoldState,
    quickSearchLaunch: QuickSearchLaunch?,
) {
    topDestinationEntry<CameraScreenRoute> { route ->
        CameraScreen(
            modifier = Modifier.keepScreenOn(),
            quickSearchLaunch = quickSearchLaunch,
            onSearchSettingEditClicked = { id -> navigator.editSearchSetting(from = route, id = id) },
            onSearchGroupEditClicked = { id -> navigator.editSearchGroup(from = route, id = id) },
        )
    }

    topDestinationEntry<OverviewScreenRoute> { route ->
        OverviewScreen(
            scaffoldState = scaffoldState,
            onSearchSettingAddClicked = { groupId -> navigator.addSearchSetting(from = route, groupId = groupId) },
            onSearchSettingEditClicked = { id -> navigator.editSearchSetting(from = route, id = id) },
            onSearchGroupAddClicked = { navigator.addSearchGroup(from = route) },
            onSearchGroupEditClicked = { id -> navigator.editSearchGroup(from = route, id = id) },
        )
    }

    editorEntry<SearchSettingScreenRoute> { route ->
        SearchSettingScreen(
            scaffoldState = scaffoldState,
            route = route,
            onExitRequested = { navigator.close(route) },
        )
    }

    editorEntry<SearchGroupScreenRoute> { route ->
        SearchGroupScreen(
            scaffoldState = scaffoldState,
            route = route,
            onExitRequested = { navigator.close(route) },
            onSearchSettingAddClicked = { groupId -> navigator.addSearchSetting(from = route, groupId = groupId) },
            onSearchSettingEditClicked = { id -> navigator.editSearchSetting(from = route, id = id) },
        )
    }
}

internal fun EntryNavigator.addSearchSetting(from: NavKey, groupId: SearchGroupId) =
    navigate(from, SearchSettingScreenRoute(groupIdDefault = groupId))

internal fun EntryNavigator.editSearchSetting(from: NavKey, id: SearchSettingId) =
    navigate(from, SearchSettingScreenRoute(id = id))

internal fun EntryNavigator.addSearchGroup(from: NavKey) =
    navigate(from, SearchGroupScreenRoute())

internal fun EntryNavigator.editSearchGroup(from: NavKey, id: SearchGroupId) =
    navigate(from, SearchGroupScreenRoute(id = id))
