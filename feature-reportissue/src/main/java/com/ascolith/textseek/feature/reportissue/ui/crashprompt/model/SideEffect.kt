package com.ascolith.textseek.feature.reportissue.ui.crashprompt.model

sealed interface SideEffect {
    data object NavigateToReportIssue : SideEffect
}
