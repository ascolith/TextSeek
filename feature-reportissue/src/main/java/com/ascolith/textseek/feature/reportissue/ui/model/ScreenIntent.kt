package com.ascolith.textseek.feature.reportissue.ui.model

sealed interface ScreenIntent {
    data class DescriptionChanged(val text: String) : ScreenIntent
    data class IncludeLogsChanged(val includeLogs: Boolean) : ScreenIntent
    data object Send : ScreenIntent
}
