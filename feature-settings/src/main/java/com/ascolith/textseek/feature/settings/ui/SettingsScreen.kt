package com.ascolith.textseek.feature.settings.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ascolith.textseek.core.composable.TsAlertDialog
import com.ascolith.textseek.core.composable.TsColorPickerDialog
import com.ascolith.textseek.core.composable.TsPreview
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.core.ui.labelResId
import com.ascolith.textseek.domain.model.TextRecognitionScript
import com.ascolith.textseek.feature.settings.ui.model.ScreenIntent
import com.ascolith.textseek.feature.settings.ui.model.SideEffect
import com.ascolith.textseek.feature.settings.ui.model.UiState
import com.ascolith.textseek.feature.settings.ui.model.TsDialog
import com.ascolith.textseek.resources.R
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun SettingsScreen(
    scaffoldState: TsScaffoldState,
    onReportIssueClicked: () -> Unit,
    onAcknowledgementsClicked: () -> Unit,
    onDebugMenuClicked: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val screenState by viewModel.collectAsState()
    val importSuccessMessage = stringResource(R.string.import_success)
    val importErrorMessage = stringResource(R.string.import_error)
    val exportErrorMessage = stringResource(R.string.export_error)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.handleIntent(ScreenIntent.ImportFileSelected(uri))
        }
    }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is SideEffect.ShareExport -> launchSettingsExportShareSheet(context, sideEffect.uri)
            is SideEffect.LaunchFilePicker -> filePickerLauncher.launch(arrayOf("application/json"))
            is SideEffect.ImportResult -> {
                val message = if (sideEffect.success) importSuccessMessage else importErrorMessage
                scope.launch {
                    scaffoldState.snackbarHostState.showSnackbar(message)
                }
            }

            is SideEffect.ExportFailed -> scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(exportErrorMessage)
            }

            SideEffect.NavigateToReportIssue -> onReportIssueClicked()
            SideEffect.NavigateToAcknowledgements -> onAcknowledgementsClicked()
            SideEffect.NavigateToDebugMenu -> onDebugMenuClicked?.invoke()
        }
    }

    SettingsScreenContent(
        modifier = modifier,
        state = screenState,
        showDebugMenu = onDebugMenuClicked != null,
        onIntent = viewModel::handleIntent,
    )
}

@Composable
internal fun SettingsScreenContent(
    state: UiState,
    showDebugMenu: Boolean,
    onIntent: (ScreenIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader(text = stringResource(R.string.search))

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(ScreenIntent.ChooseGlobalDefaultIndicatorColor) },
            trailingContent = { ColorSwatch(colorArgb = state.globalDefaultIndicatorColorArgb) },
            colors = ListItemDefaults.colors(),
            content = { Text(stringResource(R.string.default_indicator_color)) },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(ScreenIntent.ChooseTextRecognitionScript) },
            supportingContent = { Text(stringResource(state.textRecognitionScript.labelResId)) },
            colors = ListItemDefaults.colors(),
            content = { Text(stringResource(R.string.text_recognition_script)) },
        )

        SectionHeader(text = stringResource(R.string.data))

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(ScreenIntent.ExportSettings) },
            supportingContent = { Text(stringResource(R.string.export_settings_description)) },
            colors = ListItemDefaults.colors(),
            content = { Text(stringResource(R.string.export_settings)) },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(ScreenIntent.ImportSettings) },
            supportingContent = { Text(stringResource(R.string.import_settings_description)) },
            colors = ListItemDefaults.colors(),
            content = { Text(stringResource(R.string.import_settings)) },
        )

        SectionHeader(text = stringResource(R.string.about))

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(ScreenIntent.ReportIssue) },
            supportingContent = { Text(stringResource(R.string.report_issue_description)) },
            colors = ListItemDefaults.colors(),
            content = { Text(stringResource(R.string.report_issue)) },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(ScreenIntent.OpenAcknowledgements) },
            supportingContent = { Text(stringResource(R.string.acknowledgements_description)) },
            colors = ListItemDefaults.colors(),
            content = { Text(stringResource(R.string.acknowledgements)) },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        ListItem(
            modifier = Modifier.fillMaxWidth(),
            supportingContent = {
                Text(stringResource(R.string.version_format, state.versionName, state.versionCode))
            },
            colors = ListItemDefaults.colors(),
            content = { Text(stringResource(R.string.version)) },
        )

        if (showDebugMenu) {
            SectionHeader(text = stringResource(R.string.debug))

            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIntent(ScreenIntent.OpenDebugMenu) },
                supportingContent = { Text(stringResource(R.string.debug_menu_description)) },
                colors = ListItemDefaults.colors(),
                content = { Text(stringResource(R.string.debug_menu)) },
            )
        }
    }

    when (state.dialog) {
        TsDialog.ImportConfirmation -> TsAlertDialog(
            titleResId = R.string.import_confirmation,
            textResId = R.string.import_confirmation_message,
            confirmTextResId = R.string.import_action,
            confirmTextColor = MaterialTheme.colorScheme.primary,
            onConfirm = { onIntent(ScreenIntent.ConfirmImport) },
            onDismiss = { onIntent(ScreenIntent.DismissDialog) },
        )

        is TsDialog.GlobalDefaultIndicatorColorPicker -> TsColorPickerDialog(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(200.dp),
            initialColor = state.globalDefaultIndicatorColorArgb,
            selectedColor = state.dialog.selectedColorArgb,
            onColorChanged = { onIntent(ScreenIntent.GlobalDefaultIndicatorColorChanged(it)) },
            onConfirmButtonClicked = { onIntent(ScreenIntent.SaveGlobalDefaultIndicatorColor) },
            onDismissButtonClicked = { onIntent(ScreenIntent.DismissDialog) },
            onDismissRequest = { onIntent(ScreenIntent.DismissDialog) },
        )

        TsDialog.TextRecognitionScriptPicker -> TextRecognitionScriptDialog(
            selected = state.textRecognitionScript,
            onSelect = { onIntent(ScreenIntent.SelectTextRecognitionScript(it)) },
            onDismiss = { onIntent(ScreenIntent.DismissDialog) },
        )

        TsDialog.None -> Unit
    }
}

@Composable
private fun TextRecognitionScriptDialog(
    selected: TextRecognitionScript,
    onSelect: (TextRecognitionScript) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.text_recognition_script)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                TextRecognitionScript.entries.forEach { script ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = script == selected,
                                onClick = { onSelect(script) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = script == selected, onClick = null)
                        Text(
                            text = stringResource(script.labelResId),
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/** A rounded square filled with [colorArgb], outlined so light colors stay visible. */
@Composable
private fun ColorSwatch(
    colorArgb: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(shape)
            .background(Color(colorArgb))
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
    )
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

private fun launchSettingsExportShareSheet(context: Context, uri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, null))
}

@PreviewLightDark
@Composable
private fun PreviewSettingsScreen() {
    TsPreview {
        SettingsScreenContent(
            state = UiState(),
            showDebugMenu = true,
            onIntent = {},
        )
    }
}
