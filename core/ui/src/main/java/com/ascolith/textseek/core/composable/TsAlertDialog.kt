package com.ascolith.textseek.core.composable

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.ascolith.textseek.resources.R

@Composable
fun TsAlertDialog(
    @StringRes titleResId: Int,
    @StringRes textResId: Int,
    @StringRes confirmTextResId: Int = R.string.delete,
    confirmTextColor: Color = MaterialTheme.colorScheme.error,
    @StringRes dismissTextResId: Int = R.string.cancel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleResId)) },
        text = { Text(stringResource(textResId)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(confirmTextResId), color = confirmTextColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(dismissTextResId))
            }
        }
    )
}
