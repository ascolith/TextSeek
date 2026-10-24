package com.ascolith.textseek.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.ascolith.textseek.core.ui.TsTopDestination

private const val TRANSITION_DURATION_MS = 300

private const val SLIDE_FORWARD = 1
private const val SLIDE_BACK = -1

// A top-level destination's position along the bottom bar
private const val BAR_POSITION = "com.ascolith.textseek.barPosition"

internal fun barPosition(destination: TsTopDestination): Map<String, Any> =
    mapOf(BAR_POSITION to destination.ordinal)

// Slides follow the bar's order, whether the move is a push, a pop or a back gesture
internal fun AnimatedContentTransitionScope<Scene<NavKey>>.lateralSlide(): ContentTransform {
    val from = initialState.metadata[BAR_POSITION] as? Int
    val to = targetState.metadata[BAR_POSITION] as? Int
    val direction = if (from != null && to != null && to < from) SLIDE_BACK else SLIDE_FORWARD
    val slideSpec = tween<IntOffset>(TRANSITION_DURATION_MS)
    return slideInHorizontally(slideSpec) { direction * it } togetherWith
            slideOutHorizontally(slideSpec) { -direction * it }
}

private fun fallAway() = ContentTransform(
    targetContentEnter = fadeIn(tween(TRANSITION_DURATION_MS)),
    initialContentExit = slideOutVertically(tween(TRANSITION_DURATION_MS)) { it },
)

// Editors rise over the screen they were opened from and fall away to reveal it
internal val riseAndFall: Map<String, Any> =
    NavDisplay.transitionSpec {
        ContentTransform(
            targetContentEnter = slideInVertically(tween(TRANSITION_DURATION_MS)) { it },
            initialContentExit = fadeOut(tween(TRANSITION_DURATION_MS)),
        )
    } + NavDisplay.popTransitionSpec { fallAway() } +
            NavDisplay.predictivePopTransitionSpec { fallAway() }
