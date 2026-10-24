package com.ascolith.textseek.core.composable

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ascolith.textseek.core.ui.isHeightCompact
import com.ascolith.textseek.resources.R

/**
 * Explains why the camera is needed, with buttons to request access or open the app's system settings.
 * In short windows the animation moves beside the text so both fit.
 */
@Composable
fun TsCameraPermissionContent(
    showRationale: Boolean,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAnimation: Boolean = true,
) {
    if (showAnimation && isHeightCompact()) {
        Row(
            modifier = modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                TsCameraPermissionAnimation(
                    Modifier
                        .heightIn(max = 300.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true),
                )
            }
            CameraPermissionControls(
                showRationale = showRationale,
                onGrantClick = onGrantClick,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            )
        }
    } else {
        CameraPermissionControls(
            showRationale = showRationale,
            onGrantClick = onGrantClick,
            modifier = modifier.padding(16.dp),
        ) {
            if (showAnimation) {
                TsCameraPermissionAnimation(Modifier.size(300.dp))
            }
        }
    }
}

@Composable
private fun CameraPermissionControls(
    showRationale: Boolean,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
    ) {
        header()
        Text(
            text = if (showRationale) {
                stringResource(R.string.camera_permission_rationale)
            } else {
                stringResource(R.string.camera_permission_explanation)
            },
            textAlign = TextAlign.Start,
        )

        if (showRationale) {
            Text(
                text = stringResource(R.string.camera_permission_open_settings_instructions),
                textAlign = TextAlign.Start,
            )
        }
        Spacer(Modifier.height(16.dp))

        if (!showRationale) {
            Button(onClick = onGrantClick) {
                Text(text = stringResource(R.string.grant_camera_access))
            }
        }

        TextButton(onClick = { context.startActivity(appDetailsSettingsIntent(context)) }) {
            Text(stringResource(R.string.open_settings))
        }
    }
}

@Composable
fun TsCameraPermissionAnimation(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_camera_permission))
    LottieAnimation(
        composition = composition,
        modifier = modifier,
        iterations = LottieConstants.IterateForever,
    )
}

private fun appDetailsSettingsIntent(context: Context) = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", context.packageName, null),
)

@PreviewLightDark
@Composable
private fun PreviewTsCameraPermissionContentShowRationaleTrue() {
    TsPreview {
        TsCameraPermissionContent(showRationale = true, onGrantClick = {}, modifier = Modifier.fillMaxSize())
    }
}

@PreviewLightDark
@Composable
private fun PreviewTsCameraPermissionContentShowRationaleFalse() {
    TsPreview {
        TsCameraPermissionContent(showRationale = false, onGrantClick = {}, modifier = Modifier.fillMaxSize())
    }
}

@Preview(widthDp = 891, heightDp = 411)
@Composable
private fun PreviewTsCameraPermissionContentLandscape() {
    TsPreview {
        TsCameraPermissionContent(showRationale = false, onGrantClick = {}, modifier = Modifier.fillMaxSize())
    }
}
