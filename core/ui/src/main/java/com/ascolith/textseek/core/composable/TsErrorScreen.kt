package com.ascolith.textseek.core.composable

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ascolith.textseek.resources.R

@Composable
fun TsErrorScreen(
    modifier: Modifier = Modifier,
    @StringRes messageResId: Int = R.string.error_generic,
    @StringRes actionLabelResId: Int? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (actionLabelResId != null && onAction != null) {
            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = onAction,
            ) {
                Text(stringResource(actionLabelResId))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewTsErrorScreen() {
    TsPreview {
        TsErrorScreen()
    }
}

@PreviewLightDark
@Composable
private fun PreviewTsErrorScreenWithAction() {
    TsPreview {
        TsErrorScreen(
            actionLabelResId = R.string.back,
            onAction = {},
        )
    }
}
