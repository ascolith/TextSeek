package com.ascolith.textseek.core.composable

import androidx.compose.foundation.background
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.ascolith.textseek.resources.R

@Composable
fun TsColorPickerDialog(
    initialColor: Int?,
    selectedColor: Int?,
    onColorChanged: (colorArgb: Int) -> Unit,
    onConfirmButtonClicked: () -> Unit,
    onDismissRequest: () -> Unit,
    onDismissButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes resetLabelResId: Int? = null,
    onResetClicked: (() -> Unit)? = null,
    confirmButtonText: String = stringResource(R.string.save),
    dismissButtonText: String = stringResource(R.string.close),
) {
    val colorPickerController = rememberColorPickerController()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.indicator_color)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = modifier
                        .clip(shape = RoundedCornerShape(8.dp))
                        .background(color = (selectedColor ?: initialColor)?.let { Color(it) } ?: Color.Transparent)
                ) {
                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(200.dp),
                        controller = colorPickerController,
                        initialColor = initialColor?.let { Color(it) },
                        onColorChanged = { colorEnvelope: ColorEnvelope ->
                            if (colorEnvelope.fromUser) {
                                onColorChanged(colorEnvelope.color.toArgb())
                            }
                        }
                    )
                }
                if (resetLabelResId != null && onResetClicked != null) {
                    TextButton(onClick = onResetClicked) {
                        Text(text = stringResource(resetLabelResId))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmButtonClicked) {
                Text(text = confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissButtonClicked) {
                Text(text = dismissButtonText)
            }
        }
    )
}
