package com.ascolith.textseek.feature.reportissue.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ascolith.textseek.core.composable.TsPreview
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.feature.reportissue.ui.model.ScreenIntent
import com.ascolith.textseek.feature.reportissue.ui.model.SideEffect
import com.ascolith.textseek.feature.reportissue.ui.model.UiState
import com.ascolith.textseek.resources.R
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun ReportIssueScreen(
    scaffoldState: TsScaffoldState,
    modifier: Modifier = Modifier,
    viewModel: ReportIssueViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val screenState by viewModel.collectAsState()
    val noEmailAppMessage = stringResource(R.string.no_email_app, stringResource(R.string.support_email))
    val attachLogsErrorMessage = stringResource(R.string.attach_logs_error)

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            SideEffect.NoEmailApp -> scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(noEmailAppMessage)
            }

            SideEffect.AttachLogsFailed -> scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(attachLogsErrorMessage)
            }
        }
    }

    ReportIssueScreenContent(
        modifier = modifier,
        state = screenState,
        onIntent = viewModel::handleIntent,
    )
}

@Composable
internal fun ReportIssueScreenContent(
    state: UiState,
    onIntent: (ScreenIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Held locally rather than read back from the state: Orbit reduces asynchronously, and
    // feeding the field from a lagging value makes the cursor jump while typing.
    var description by rememberSaveable { mutableStateOf(state.description) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            value = description,
            onValueChange = {
                description = it.take(UiState.MAX_DESCRIPTION_LENGTH)
                onIntent(ScreenIntent.DescriptionChanged(description))
            },
            label = { Text(stringResource(R.string.issue_description)) },
            placeholder = { Text(stringResource(R.string.report_issue_hint)) },
            supportingText = {
                Text(
                    text = stringResource(
                        R.string.character_count_format,
                        description.length,
                        UiState.MAX_DESCRIPTION_LENGTH,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = state.includeLogs,
                    role = Role.Checkbox,
                    onValueChange = { onIntent(ScreenIntent.IncludeLogsChanged(it)) },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = state.includeLogs, onCheckedChange = null)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = stringResource(R.string.include_logs),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.include_logs_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSend,
            onClick = { onIntent(ScreenIntent.Send) },
        ) {
            Text(stringResource(R.string.send_email))
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewReportIssueScreen() {
    TsPreview {
        ReportIssueScreenContent(
            state = UiState(description = "The camera freezes when I rotate the phone."),
            onIntent = {},
        )
    }
}
