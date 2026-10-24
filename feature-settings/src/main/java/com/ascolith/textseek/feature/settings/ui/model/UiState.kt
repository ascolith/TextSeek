package com.ascolith.textseek.feature.settings.ui.model

import com.ascolith.textseek.domain.model.AppSetting
import com.ascolith.textseek.domain.model.TextRecognitionScript

data class UiState(
    val dialog: TsDialog = TsDialog.None,
    val versionName: String = "",
    val versionCode: Long = 0,
    val globalDefaultIndicatorColorArgb: Int = AppSetting.GlobalDefaultIndicatorColor.defaultValue,
    val textRecognitionScript: TextRecognitionScript = AppSetting.TextRecognitionScript.defaultValue,
)
