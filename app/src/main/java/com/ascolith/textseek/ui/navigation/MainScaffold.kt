package com.ascolith.textseek.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.core.ui.TsNavigationBar
import com.ascolith.textseek.core.ui.TsNavigationRail
import com.ascolith.textseek.core.ui.TsTopDestination
import com.ascolith.textseek.core.ui.isHeightCompact
import com.ascolith.textseek.core.ui.tsNavigationRailInsets
import com.ascolith.textseek.resources.R

/** Navigation shows only on top destinations that ask for it, as a rail in short windows. */
@Composable
internal fun MainScaffold(
    scaffoldState: TsScaffoldState,
    currentTopDestination: TsTopDestination?,
    onBack: () -> Unit,
    onTopDestinationSelected: (TsTopDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val showNavigation = currentTopDestination != null && scaffoldState.config.showBottomBar
    val useRail = showNavigation && isHeightCompact()
    val onNavigate: (TsTopDestination) -> Unit = { destination ->
        if (destination != currentTopDestination) {
            onTopDestinationSelected(destination)
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
            TsNavigationRail(
                currentDestination = currentTopDestination,
                onNavigate = onNavigate,
            )
        }
        Scaffold(
            modifier = Modifier
                .weight(1f)
                // The rail already pads by the start insets
                .then(
                    if (useRail) {
                        Modifier.consumeWindowInsets(tsNavigationRailInsets.only(WindowInsetsSides.Start))
                    } else {
                        Modifier
                    }
                ),
            snackbarHost = { SnackbarHost(scaffoldState.snackbarHostState) },
            topBar = { MainTopBar(scaffoldState = scaffoldState, onBack = onBack) },
            floatingActionButton = { MainFloatingActionButton(scaffoldState = scaffoldState) },
            bottomBar = {
                if (showNavigation && !useRail) {
                    TsNavigationBar(
                        currentDestination = currentTopDestination,
                        onNavigate = onNavigate,
                    )
                }
            },
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    scaffoldState: TsScaffoldState,
    onBack: () -> Unit,
) {
    val titleResId = scaffoldState.config.topBarTitleResId ?: return

    TopAppBar(
        navigationIcon = {
            if (scaffoldState.config.showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(titleResId),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
        },
        actions = {
            scaffoldState.config.actions.forEachIndexed { index, action ->
                IconButton(onClick = { scaffoldState.actionClickListener?.invoke(index) }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(action.iconResId),
                        contentDescription = stringResource(action.labelResId),
                    )
                }
            }
        },
    )
}

@Composable
private fun MainFloatingActionButton(scaffoldState: TsScaffoldState) {
    val fabConfig = scaffoldState.config.fabConfig ?: return

    ExtendedFloatingActionButton(
        onClick = { scaffoldState.fabClickListener?.invoke() },
        text = {
            fabConfig.textResId?.let { text -> Text(stringResource(text)) }
        },
        icon = {
            Icon(
                imageVector = ImageVector.vectorResource(fabConfig.iconResId),
                contentDescription = fabConfig.contentDescription,
            )
        }
    )
}
