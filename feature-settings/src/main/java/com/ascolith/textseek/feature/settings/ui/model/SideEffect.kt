package com.ascolith.textseek.feature.settings.ui.model

import android.net.Uri

sealed interface SideEffect {
    data class ShareExport(val uri: Uri) : SideEffect
    data object LaunchFilePicker : SideEffect
    data class ImportResult(val success: Boolean) : SideEffect
    data object ExportFailed : SideEffect
    data object NavigateToReportIssue : SideEffect
    data object NavigateToAcknowledgements : SideEffect
    data object OpenPrivacyPolicy : SideEffect
    data object OpenLicense : SideEffect
    data object NavigateToDebugMenu : SideEffect
}
