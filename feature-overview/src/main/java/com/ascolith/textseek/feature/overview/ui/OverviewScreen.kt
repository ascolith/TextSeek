@file:OptIn(ExperimentalMaterial3Api::class)

package com.ascolith.textseek.feature.overview.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ascolith.textseek.core.composable.TsLoadingScreen
import com.ascolith.textseek.core.composable.TsErrorScreen
import com.ascolith.textseek.core.composable.TsPreview
import com.ascolith.textseek.core.composable.TsSearchGroupItem
import com.ascolith.textseek.core.composable.TsSearchGroupItemData
import com.ascolith.textseek.core.composable.TsSearchSettingItemData
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.overview.ui.composable.mockOverviewItems
import com.ascolith.textseek.feature.overview.ui.model.OverviewItemUiModel
import com.ascolith.textseek.feature.overview.ui.model.ScreenIntent
import com.ascolith.textseek.feature.overview.ui.model.SearchSettingUiModel
import com.ascolith.textseek.feature.overview.ui.model.SideEffect
import com.ascolith.textseek.feature.overview.ui.model.TransientScreen
import com.ascolith.textseek.feature.overview.ui.navigation.OverviewScreenRoute
import com.ascolith.textseek.resources.R
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun OverviewScreen(
    scaffoldState: TsScaffoldState,
    onSearchGroupAddClicked: () -> Unit,
    onSearchGroupEditClicked: (groupId: SearchGroupId) -> Unit,
    onSearchSettingAddClicked: (groupId: SearchGroupId) -> Unit,
    onSearchSettingEditClicked: (SearchSettingId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = hiltViewModel(),
) {
    val screenState by viewModel.collectAsState()

    viewModel.collectSideEffect {
        when (it) {
            SideEffect.NavigateToSearchGroupAdd -> onSearchGroupAddClicked()
            is SideEffect.NavigateToSearchGroupEdit -> onSearchGroupEditClicked(it.id)
            is SideEffect.NavigateToSearchSettingAdd -> onSearchSettingAddClicked(it.groupId)
            is SideEffect.NavigateToSearchSettingEdit -> onSearchSettingEditClicked(it.id)
        }
    }

    SideEffect {
        scaffoldState.updateFabClickListener(OverviewScreenRoute) {
            viewModel.handleIntent(ScreenIntent.SearchGroupAddClicked)
        }
    }

    when (screenState.transientScreen) {
        is TransientScreen.Loading ->
            TsLoadingScreen(modifier = modifier.fillMaxSize())

        is TransientScreen.Error ->
            TsErrorScreen(modifier = modifier.fillMaxSize())

        is TransientScreen.None ->
            OverviewScreen(
                modifier = modifier,
                overviewItems = screenState.overviewItems.values.toList(),
                onIntent = { viewModel.handleIntent(it) },
            )
    }
}

@Composable
internal fun OverviewScreen(
    overviewItems: List<OverviewItemUiModel>,
    onIntent: (ScreenIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (overviewItems.isEmpty()) {
        OverviewEmptyState(modifier = modifier)
    } else {
        OverviewLazyColumn(
            modifier = modifier,
            overviewItems = overviewItems,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun OverviewEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(32.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.add_group_to_get_started),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OverviewLazyColumn(
    overviewItems: List<OverviewItemUiModel>,
    onIntent: (ScreenIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(
            overviewItems,
            key = { it.group.id.value }
        ) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { onIntent(ScreenIntent.SearchGroupEditClicked(item.group.id)) },
            ) {
                TsSearchGroupItem(
                    data = item.toTsOverviewGroupData(),
                    onGroupEnabledToggle = { groupId -> onIntent(ScreenIntent.ToggleSearchGroupEnabled(groupId)) },
                    onSearchSettingAddClick = { groupId -> onIntent(ScreenIntent.SearchSettingAddClicked(groupId)) },
                    onSearchSettingEditClick = { settingId ->
                        onIntent(ScreenIntent.SearchSettingEditClicked(settingId))
                    },
                )
            }
        }
    }
}

private fun OverviewItemUiModel.toTsOverviewGroupData() =
    TsSearchGroupItemData(
        searchGroupId = group.id,
        name = group.name,
        enabled = group.enabled,
        groupColorArgb = group.groupColorArgb,
        settings = settings.mapValues { it.value.toTsOverviewSettingData() }
    )

private fun SearchSettingUiModel.toTsOverviewSettingData() =
    TsSearchSettingItemData(
        searchSettingId = id,
        text = text,
        enabled = enabled,
        colorArgb = colorArgb,
    )

@PreviewLightDark
@Composable
private fun PreviewOverviewScreen() {
    TsPreview {
        OverviewScreen(
            overviewItems = mockOverviewItems.values.toList(),
            onIntent = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewOverviewScreenEmpty() {
    TsPreview {
        OverviewScreen(
            overviewItems = emptyList(),
            onIntent = {}
        )
    }
}
