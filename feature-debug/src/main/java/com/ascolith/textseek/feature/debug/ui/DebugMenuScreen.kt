package com.ascolith.textseek.feature.debug.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ascolith.textseek.core.composable.TsPreview
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.feature.debug.R
import com.ascolith.textseek.feature.debug.ui.model.ScreenIntent
import com.ascolith.textseek.feature.debug.ui.model.SideEffect
import com.ascolith.textseek.feature.debug.ui.model.UiState
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun DebugMenuScreen(
    scaffoldState: TsScaffoldState,
    modifier: Modifier = Modifier,
    viewModel: DebugMenuViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val screenState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            // Posted rather than thrown here, so it reaches the main thread's uncaught
            // exception handler instead of the side effect coroutine's.
            SideEffect.Crash -> Handler(Looper.getMainLooper()).post { throw DebugMenuForcedCrash() }
            is SideEffect.ShowMessage -> scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(context.getString(sideEffect.messageResId))
            }
        }
    }

    DebugMenuScreenContent(
        modifier = modifier,
        state = screenState,
        onIntent = viewModel::handleIntent,
    )
}

@Composable
internal fun DebugMenuScreenContent(
    state: UiState,
    onIntent: (ScreenIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader(text = stringResource(R.string.debug_section_crash_reporting))

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(ScreenIntent.ForceCrash) },
            supportingContent = { Text(stringResource(R.string.debug_force_crash_description)) },
            colors = ListItemDefaults.colors(),
            content = { Text(stringResource(R.string.debug_force_crash)) },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(ScreenIntent.ClearPendingCrash) },
            supportingContent = {
                val statusResId =
                    if (state.hasPendingCrash) R.string.debug_pending_crash else R.string.debug_no_pending_crash
                Text(stringResource(statusResId))
            },
            colors = ListItemDefaults.colors(),
            content = { Text(stringResource(R.string.debug_clear_pending_crash)) },
        )

        SectionHeader(text = stringResource(R.string.debug_section_data))

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(ScreenIntent.SeedSampleData) },
            supportingContent = { Text(stringResource(R.string.debug_seed_sample_data_description)) },
            colors = ListItemDefaults.colors(),
            content = { Text(stringResource(R.string.debug_seed_sample_data)) },
        )

        SectionHeader(text = stringResource(R.string.debug_section_onboarding))

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(ScreenIntent.ResetOnboarding) },
            supportingContent = { Text(stringResource(R.string.debug_reset_onboarding_description)) },
            colors = ListItemDefaults.colors(),
            content = { Text(stringResource(R.string.debug_reset_onboarding)) },
        )
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@PreviewLightDark
@Composable
private fun PreviewDebugMenuScreen() {
    TsPreview {
        DebugMenuScreenContent(
            state = UiState(hasPendingCrash = true),
            onIntent = {},
        )
    }
}
