package com.ascolith.textseek.feature.settings.ui.model

import android.net.Uri
import com.ascolith.textseek.domain.model.TextRecognitionScript

sealed interface ScreenIntent {
    data object ExportSettings : ScreenIntent
    data object ImportSettings : ScreenIntent
    data object ConfirmImport : ScreenIntent
    data object DismissDialog : ScreenIntent
    data class ImportFileSelected(val uri: Uri) : ScreenIntent
    data object ChooseGlobalDefaultIndicatorColor : ScreenIntent
    data class GlobalDefaultIndicatorColorChanged(val colorArgb: Int) : ScreenIntent
    data object SaveGlobalDefaultIndicatorColor : ScreenIntent
    data object ChooseTextRecognitionScript : ScreenIntent
    data class SelectTextRecognitionScript(val script: TextRecognitionScript) : ScreenIntent
    data object ReportIssue : ScreenIntent
    data object OpenAcknowledgements : ScreenIntent
    data object OpenDebugMenu : ScreenIntent
}
