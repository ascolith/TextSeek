package com.ascolith.textseek.feature.searchgroup.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ascolith.textseek.core.composable.TsAlertDialog
import com.ascolith.textseek.core.composable.TsColorPickerDialog
import com.ascolith.textseek.core.composable.TsColorSetting
import com.ascolith.textseek.core.composable.TsLoadingScreen
import com.ascolith.textseek.core.composable.TsErrorScreen
import com.ascolith.textseek.core.composable.TsPreview
import com.ascolith.textseek.core.composable.TsSearchSettingItemData
import com.ascolith.textseek.core.composable.TsSearchSettingList
import com.ascolith.textseek.core.composable.TsSettingTextField
import com.ascolith.textseek.core.composable.TsSwitchSetting
import com.ascolith.textseek.core.model.ActionConfig
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.searchgroup.ui.model.ScreenIntent
import com.ascolith.textseek.feature.searchgroup.ui.model.SideEffect
import com.ascolith.textseek.feature.searchgroup.ui.model.TransientScreen
import com.ascolith.textseek.feature.searchgroup.ui.model.UiState
import com.ascolith.textseek.feature.searchgroup.ui.model.TsDialog
import com.ascolith.textseek.feature.searchgroup.ui.navigation.SearchGroupScreenRoute
import com.ascolith.textseek.resources.R
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun SearchGroupScreen(
    scaffoldState: TsScaffoldState,
    route: SearchGroupScreenRoute,
    onExitRequested: () -> Unit,
    onSearchSettingAddClicked: (groupId: SearchGroupId) -> Unit,
    onSearchSettingEditClicked: (SearchSettingId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchGroupViewModel = hiltViewModel<SearchGroupViewModel, SearchGroupViewModel.Factory> { factory ->
        factory.create(route)
    },
) {

    val screenState by viewModel.collectAsState()

    viewModel.collectSideEffect {
        when (it) {
            SideEffect.Exit -> onExitRequested()
            is SideEffect.NavigateToSearchSettingAdd -> onSearchSettingAddClicked(it.groupId)
            is SideEffect.NavigateToSearchSettingEdit -> onSearchSettingEditClicked(it.settingId)
        }
    }

    BackHandler {
        viewModel.handleIntent(ScreenIntent.Exit)
    }

    SideEffect {
        // Each action carries the intent it fires, so the two stay together and the
        // dispatch does not have to recover which one was tapped. Matching on iconResId
        // made the icon the action's identity, which breaks silently the moment two
        // actions share one.
        val groupId = screenState.id
        val actions: List<Pair<ActionConfig, () -> Unit>> = buildList {
            if (groupId != null) {
                add(
                    ActionConfig(
                        iconResId = R.drawable.ic_add,
                        labelResId = R.string.add_search_term,
                    ) to { viewModel.handleIntent(ScreenIntent.SearchSettingAddClicked(groupId)) }
                )
                add(
                    ActionConfig(
                        iconResId = R.drawable.ic_delete,
                        labelResId = R.string.delete,
                    ) to { viewModel.handleIntent(ScreenIntent.DeleteSearchGroup) }
                )
            }
        }
        scaffoldState.updateConfig(
            owner = route,
            newConfig = route.getScaffoldConfig().copy(actions = actions.map { it.first }),
        )
        scaffoldState.updateActionClickListener(route) { index ->
            actions.getOrNull(index)?.second?.invoke()
        }
    }

    when (screenState.transientScreen) {
        is TransientScreen.Loading ->
            TsLoadingScreen(modifier = modifier.fillMaxSize())

        is TransientScreen.NotFound ->
            TsErrorScreen(
                modifier = modifier.fillMaxSize(),
                messageResId = R.string.error_search_group_missing,
                actionLabelResId = R.string.back,
                onAction = onExitRequested,
            )

        is TransientScreen.Error ->
            TsErrorScreen(
                modifier = modifier.fillMaxSize(),
                actionLabelResId = R.string.back,
                onAction = onExitRequested,
            )

        is TransientScreen.None ->
            SearchGroupScreen(
                modifier = modifier,
                state = screenState,
                onIntent = viewModel::handleIntent,
            )
    }

}

@Composable
internal fun SearchGroupScreen(
    state: UiState,
    onIntent: (ScreenIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .imePadding()
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        TsSettingTextField(
            modifier = Modifier.fillMaxWidth(),
            initialText = state.name,
            label = { Text(stringResource(R.string.group_name)) },
            strikeThroughText = !state.enabled,
            onValueChange = {
                onIntent(ScreenIntent.UpdateSearchGroupName(it))
            }
        )

        TsColorSetting(
            labelResId = R.string.default_indicator_color,
            colorArgb = state.colorArgb,
            onChooseClicked = { onIntent(ScreenIntent.ShowDialog(TsDialog.ColorPicker())) },
        )

        TsSwitchSetting(
            labelResId = R.string.enabled,
            checked = state.enabled,
            onToggled = { onIntent(ScreenIntent.ToggleSearchGroupEnabled) },
        )

        TsSearchSettingList(
            modifier = Modifier.weight(1f),
            settings = state.searchSettings.values.toList(),
            defaultColor = state.colorArgb,
            onItemClick = { onIntent(ScreenIntent.SearchSettingEditClicked(it)) }
        )

        Button(
            onClick = { onIntent(ScreenIntent.Save) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.name.isNotBlank(),
        ) {
            Text(stringResource(R.string.save))
        }
    }

    when (state.dialog) {
        is TsDialog.DeleteConfirmation -> TsAlertDialog(
            titleResId = R.string.delete_group,
            textResId = R.string.delete_group_confirmation,
            onConfirm = { onIntent(ScreenIntent.ConfirmDeleteSearchGroup) },
            onDismiss = { onIntent(ScreenIntent.HideDialog) },
        )

        is TsDialog.ColorPicker -> TsColorPickerDialog(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(200.dp),
            initialColor = state.colorArgb,
            selectedColor = state.dialog.selectedColorArgb,
            onColorChanged = { onIntent(ScreenIntent.UpdateColorPickerSelectedColor(it)) },
            onConfirmButtonClicked = { onIntent(ScreenIntent.SaveSearchGroupColor) },
            onDismissButtonClicked = { onIntent(ScreenIntent.HideDialog) },
            onDismissRequest = { onIntent(ScreenIntent.HideDialog) },
        )

        TsDialog.None -> Unit // no-op
    }
}


@PreviewLightDark
@Composable
private fun PreviewSearchGroupScreen() {
    TsPreview {
        SearchGroupScreen(
            state = UiState(
                dialog = TsDialog.None,
                id = SearchGroupId(1),
                name = "Allergens",
                enabled = true,
                colorArgb = Color.Blue.toArgb(),
                searchSettings = listOf(
                    TsSearchSettingItemData(
                        searchSettingId = SearchSettingId(0),
                        text = "Banana",
                        enabled = true,
                        colorArgb = Color.Yellow.toArgb(),
                    ),
                    TsSearchSettingItemData(
                        searchSettingId = SearchSettingId(1),
                        text = "Apple",
                        enabled = true,
                        colorArgb = Color.Red.toArgb(),
                    ),
                    TsSearchSettingItemData(
                        searchSettingId = SearchSettingId(2),
                        text = "Grape",
                        enabled = true,
                        colorArgb = Color.Blue.toArgb(),
                    ),
                ).associateBy { it.searchSettingId }
            ),
            onIntent = {},
        )
    }
}
