package com.ascolith.textseek.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** Material's compact-height breakpoint. */
private val COMPACT_HEIGHT_MAX = 480.dp

/** Whether the window is short enough to need a landscape layout, e.g. a phone in landscape. */
@Composable
fun isHeightCompact(): Boolean = LocalWindowInfo.current.containerDpSize.height < COMPACT_HEIGHT_MAX
