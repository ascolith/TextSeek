package com.ascolith.textseek.feature.camerascreen.ui

import androidx.camera.viewfinder.core.FocusState
import androidx.camera.viewfinder.core.FocusStateValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

internal data class FocusIndicatorState(
    val position: Offset,
    @param:FocusStateValue val focusState: Int,
)

@Composable
internal fun FocusIndicator(
    indicator: FocusIndicatorState,
    onHidden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(indicator) {
        if (indicator.focusState != FocusState.STARTED) {
            delay(FOCUS_RESULT_DISPLAY_DURATION_MS)
            onHidden()
        }
    }

    val scale = remember(indicator.position) { Animatable(1.4f) }
    LaunchedEffect(indicator.position) {
        scale.animateTo(1f, animationSpec = tween(durationMillis = 200))
    }

    val color = when (indicator.focusState) {
        FocusState.FOCUSED -> MaterialTheme.colorScheme.primary
        FocusState.NOT_FOCUSED, FocusState.FAILED -> MaterialTheme.colorScheme.error
        else -> Color.White
    }

    Box(
        modifier = modifier
            .offset {
                val halfSize = (FOCUS_INDICATOR_SIZE / 2).toPx()
                IntOffset(
                    x = (indicator.position.x - halfSize).roundToInt(),
                    y = (indicator.position.y - halfSize).roundToInt(),
                )
            }
            .size(FOCUS_INDICATOR_SIZE)
            .scale(scale.value)
            .border(width = 2.dp, color = color, shape = CircleShape),
    )
}

private val FOCUS_INDICATOR_SIZE = 64.dp
private const val FOCUS_RESULT_DISPLAY_DURATION_MS = 800L
