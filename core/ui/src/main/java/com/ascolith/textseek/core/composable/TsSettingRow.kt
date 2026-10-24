package com.ascolith.textseek.core.composable

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ascolith.textseek.core.ui.ColorCalculation
import com.ascolith.textseek.resources.R

@Composable
fun TsSettingRow(
    @StringRes labelResId: Int,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(labelResId),
            modifier = Modifier.padding(end = 16.dp),
        )
        trailing()
    }
}

@Composable
fun TsSwitchSetting(
    @StringRes labelResId: Int,
    checked: Boolean,
    onToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TsSettingRow(
        labelResId = labelResId,
        modifier = modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = { onToggled() },
        ),
    ) {
        Switch(
            modifier = Modifier.padding(vertical = 8.dp),
            checked = checked,
            onCheckedChange = null,
        )
    }
}

/** A settings row whose control is a button filled with the currently chosen color. */
@Composable
fun TsColorSetting(
    @StringRes labelResId: Int,
    @ColorInt colorArgb: Int,
    onChooseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetColor = Color(colorArgb)
    val indicatorColor by animateColorAsState(targetColor)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val buttonTextColor = remember(targetColor, surfaceColor) {
        ColorCalculation.getBestForegroundColor(targetColor, surfaceColor)
    }
    val animatedButtonTextColor by animateColorAsState(buttonTextColor)

    TsSettingRow(labelResId = labelResId, modifier = modifier) {
        Button(
            onClick = onChooseClicked,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
            colors = ButtonDefaults.buttonColors(containerColor = indicatorColor),
        ) {
            Text(
                text = stringResource(R.string.choose_color),
                color = animatedButtonTextColor,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewTsSettingRows() {
    TsPreview {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TsSwitchSetting(
                labelResId = R.string.enabled,
                checked = true,
                onToggled = {},
            )
            TsSwitchSetting(
                labelResId = R.string.case_sensitive,
                checked = false,
                onToggled = {},
            )
            TsColorSetting(
                labelResId = R.string.indicator_color,
                colorArgb = Color.Yellow.toArgb(),
                onChooseClicked = {},
            )
            TsColorSetting(
                labelResId = R.string.default_indicator_color,
                colorArgb = Color(0xFF1B4D9B).toArgb(),
                onChooseClicked = {},
            )
        }
    }
}
