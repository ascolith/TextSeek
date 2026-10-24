package com.ascolith.textseek.feature.camerascreen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ascolith.textseek.core.composable.TsColorPickerDialog
import com.ascolith.textseek.core.composable.TsColorSetting
import com.ascolith.textseek.core.composable.TsPreview
import com.ascolith.textseek.core.composable.TsSettingTextField
import com.ascolith.textseek.core.composable.TsSwitchSetting
import com.ascolith.textseek.feature.camerascreen.ui.model.ScreenIntent
import com.ascolith.textseek.feature.camerascreen.ui.model.TsDialog
import com.ascolith.textseek.resources.R

@Composable
internal fun QuickSearchDialog(
    dialog: TsDialog.QuickSearch,
    onIntent: (ScreenIntent.QuickSearch) -> Unit,
) {
    val autoFocusText = remember { dialog.text.isEmpty() }

    AlertDialog(
        onDismissRequest = { onIntent(ScreenIntent.QuickSearch.Dismiss) },
        title = { Text(stringResource(R.string.quick_search)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TsSettingTextField(
                    modifier = Modifier.fillMaxWidth(),
                    initialText = dialog.text,
                    label = { Text(stringResource(R.string.search_text)) },
                    onValueChange = { onIntent(ScreenIntent.QuickSearch.UpdateText(it)) },
                    autoFocus = autoFocusText,
                )

                TsColorSetting(
                    labelResId = R.string.indicator_color,
                    colorArgb = dialog.colorArgb,
                    onChooseClicked = { onIntent(ScreenIntent.QuickSearch.ShowColorPicker) },
                )

                TsSwitchSetting(
                    labelResId = R.string.case_sensitive,
                    checked = dialog.caseSensitive,
                    onToggled = { onIntent(ScreenIntent.QuickSearch.ToggleCaseSensitive) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onIntent(ScreenIntent.QuickSearch.Start) },
                enabled = dialog.text.isNotBlank(),
            ) {
                Text(stringResource(R.string.start))
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(ScreenIntent.QuickSearch.Dismiss) }) {
                Text(stringResource(R.string.cancel))
            }
        },
    )

    if (dialog.isColorPickerVisible) {
        TsColorPickerDialog(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(200.dp),
            initialColor = dialog.colorArgb,
            selectedColor = dialog.colorPickerSelectedColorArgb,
            onColorChanged = { onIntent(ScreenIntent.QuickSearch.UpdateColorPickerColor(it)) },
            onConfirmButtonClicked = { onIntent(ScreenIntent.QuickSearch.SaveColor) },
            onDismissButtonClicked = { onIntent(ScreenIntent.QuickSearch.HideColorPicker) },
            onDismissRequest = { onIntent(ScreenIntent.QuickSearch.HideColorPicker) },
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewQuickSearchDialog() {
    TsPreview {
        QuickSearchDialog(
            dialog = TsDialog.QuickSearch(
                colorArgb = Color.Yellow.toArgb(),
                text = "Example",
                caseSensitive = true,
            ),
            onIntent = {},
        )
    }
}
