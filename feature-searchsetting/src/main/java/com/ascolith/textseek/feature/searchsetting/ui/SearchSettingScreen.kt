package com.ascolith.textseek.feature.searchsetting.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ascolith.textseek.core.composable.TsAlertDialog
import com.ascolith.textseek.core.composable.TsColorPickerDialog
import com.ascolith.textseek.core.composable.TsColorSetting
import com.ascolith.textseek.core.composable.TsLoadingScreen
import com.ascolith.textseek.core.composable.TsErrorScreen
import com.ascolith.textseek.core.composable.TsPreview
import com.ascolith.textseek.core.composable.TsSettingTextField
import com.ascolith.textseek.core.composable.TsSwitchSetting
import com.ascolith.textseek.core.model.ActionConfig
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.searchsetting.ui.model.ScreenIntent
import com.ascolith.textseek.feature.searchsetting.ui.model.SelectedColor
import com.ascolith.textseek.feature.searchsetting.ui.model.SideEffect
import com.ascolith.textseek.feature.searchsetting.ui.model.TransientScreen
import com.ascolith.textseek.feature.searchsetting.ui.model.UiState
import com.ascolith.textseek.feature.searchsetting.ui.model.TsDialog
import com.ascolith.textseek.feature.searchsetting.ui.navigation.SearchSettingScreenRoute
import com.ascolith.textseek.resources.R
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun SearchSettingScreen(
    scaffoldState: TsScaffoldState,
    route: SearchSettingScreenRoute,
    onExitRequested: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchSettingViewModel =
        hiltViewModel<SearchSettingViewModel, SearchSettingViewModel.Factory> { factory ->
        factory.create(route)
    },
) {
    val screenState by viewModel.collectAsState()

    SideEffect {
        val actions: List<Pair<ActionConfig, () -> Unit>> = buildList {
            if (screenState.id != null) {
                add(
                    ActionConfig(
                        iconResId = R.drawable.ic_delete,
                        labelResId = R.string.delete,
                    ) to { viewModel.handleIntent(ScreenIntent.DeleteSearchSetting) }
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

    viewModel.collectSideEffect {
        when (it) {
            SideEffect.Exit -> onExitRequested()
        }
    }

    BackHandler {
        viewModel.handleIntent(ScreenIntent.Exit)
    }

    when (screenState.transientScreen) {
        is TransientScreen.Loading ->
            TsLoadingScreen(modifier = modifier.fillMaxSize())

        is TransientScreen.NotFound ->
            TsErrorScreen(
                modifier = modifier.fillMaxSize(),
                messageResId = R.string.error_search_setting_missing,
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
            SearchSettingScreen(
                modifier = modifier,
                state = screenState,
                onIntent = viewModel::handleIntent,
            )
    }
}

@Composable
internal fun SearchSettingScreen(
    state: UiState,
    onIntent: (ScreenIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TsSettingTextField(
            modifier = Modifier.fillMaxWidth(),
            initialText = state.text,
            label = { Text(stringResource(R.string.search_text)) },
            strikeThroughText = !state.enabled,
            onValueChange = {
                onIntent(ScreenIntent.UpdateSearchSettingText(it))
            },
        )

        GroupSetting(
            selectedGroupName = state.selectedSearchGroup?.name,
            onIntent = onIntent,
        )

        TsColorSetting(
            labelResId = R.string.indicator_color,
            colorArgb = state.selectedColor.require(),
            onChooseClicked = { onIntent(ScreenIntent.ShowDialog(TsDialog.ColorPicker())) },
        )

        TsSwitchSetting(
            labelResId = R.string.case_sensitive,
            checked = state.caseSensitive,
            onToggled = { onIntent(ScreenIntent.ToggleSearchSettingCaseSensitive) },
        )

        TsSwitchSetting(
            labelResId = R.string.enabled,
            checked = state.enabled,
            onToggled = { onIntent(ScreenIntent.ToggleSearchSettingEnabled) },
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onIntent(ScreenIntent.Save) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.text.isNotBlank() && state.selectedSearchGroup != null,
        ) {
            Text(stringResource(R.string.save))
        }
    }

    when (state.dialog) {
        is TsDialog.ColorPicker -> {
            val canUseGroupColor = state.selectedColor.groupColorArgb != null
            TsColorPickerDialog(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(200.dp),
                initialColor = state.selectedColor.require(),
                selectedColor = state.dialog.selectedColorArgb,
                resetLabelResId = R.string.use_group_color.takeIf { canUseGroupColor },
                onResetClicked = if (canUseGroupColor) {
                    { onIntent(ScreenIntent.UseGroupColor) }
                } else {
                    null
                },
                onColorChanged = { onIntent(ScreenIntent.UpdateColorPickerSelectedColor(it)) },
                onConfirmButtonClicked = { onIntent(ScreenIntent.SaveSearchSettingColor) },
                onDismissButtonClicked = { onIntent(ScreenIntent.HideDialog) },
                onDismissRequest = { onIntent(ScreenIntent.HideDialog) },
            )
        }

        is TsDialog.GroupPicker -> GroupPickerDialog(
            searchGroups = state.searchGroups,
            selectedGroupId = state.selectedSearchGroup?.id,
            onIntent = onIntent,
        )

        TsDialog.DeleteConfirmation -> TsAlertDialog(
            titleResId = R.string.delete_search,
            textResId = R.string.delete_search_confirmation,
            onConfirm = { onIntent(ScreenIntent.ConfirmDeleteSearchSetting) },
            onDismiss = { onIntent(ScreenIntent.HideDialog) },
        )

        TsDialog.None -> Unit // no-op
    }
}

@Composable
private fun GroupPickerDialog(
    searchGroups: List<SearchGroup>,
    selectedGroupId: SearchGroupId?,
    onIntent: (ScreenIntent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onIntent(ScreenIntent.HideDialog) },
        title = { Text(stringResource(R.string.choose_group)) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                items(searchGroups) { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedGroupId == group.id,
                                onClick = {
                                    onIntent(ScreenIntent.UpdateSearchSettingGroup(group.id))
                                },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGroupId == group.id,
                            onClick = null
                        )
                        Text(
                            text = group.name,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onIntent(ScreenIntent.HideDialog) }
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun GroupSetting(
    selectedGroupName: String?,
    onIntent: (ScreenIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.group),
            modifier = Modifier.padding(end = 16.dp)
        )

        Button(
            onClick = { onIntent(ScreenIntent.ShowDialog(TsDialog.GroupPicker)) },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
            colors = ButtonDefaults.outlinedButtonColors(),
        ) {
            Text(
                text = selectedGroupName ?: stringResource(R.string.choose_group)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewSearchSettingScreenColorChosen() {
    TsPreview {
        SearchSettingScreen(
            state = UiState(
                id = SearchSettingId(1),
                text = "Corn",
                caseSensitive = false,
                selectedColor = SelectedColor(
                    settingColorArgb = Color.Yellow.toArgb(),
                    groupColorArgb = Color.Blue.toArgb(),

                    ),
                searchGroups = listOf(
                    SearchGroup(
                        id = SearchGroupId(1),
                        name = "Groceries",
                        enabled = true,
                        colorArgb = Color.Red.toArgb(),
                    ),
                    SearchGroup(id = SearchGroupId(2), name = "Work", enabled = true, colorArgb = Color.Blue.toArgb()),
                    SearchGroup(
                        id = SearchGroupId(3),
                        name = "Personal",
                        enabled = true,
                        colorArgb = Color.Red.toArgb()
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewSearchSettingScreenColorNotChosen() {
    TsPreview {
        SearchSettingScreen(
            state = UiState(
                id = SearchSettingId(1),
                text = "Corn",
                caseSensitive = false,
                selectedColor = SelectedColor(
                    groupColorArgb = Color.Blue.toArgb(),
                ),
                searchGroups = listOf(
                    SearchGroup(
                        id = SearchGroupId(1),
                        name = "Groceries",
                        enabled = true,
                        colorArgb = Color.Green.toArgb(),
                    ),
                    SearchGroup(
                        id = SearchGroupId(2),
                        name = "Work",
                        enabled = true,
                        colorArgb = Color.Blue.toArgb()
                    ),
                    SearchGroup(
                        id = SearchGroupId(3),
                        name = "Personal",
                        enabled = true,
                        colorArgb = Color.Cyan.toArgb()
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}
