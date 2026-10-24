package com.ascolith.textseek.feature.reportissue.ui.crashprompt.model

sealed interface ScreenIntent {
    data object Report : ScreenIntent
    data object Dismiss : ScreenIntent
}
