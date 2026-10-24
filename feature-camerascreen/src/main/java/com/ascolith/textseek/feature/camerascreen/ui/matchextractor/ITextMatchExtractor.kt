package com.ascolith.textseek.feature.camerascreen.ui.matchextractor

import com.ascolith.textseek.domain.model.RecognizedText
import com.ascolith.textseek.feature.camerascreen.ui.model.SearchSettingUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchUiModel

fun interface ITextMatchExtractor {
    fun extract(
        recognizedTexts: List<RecognizedText>,
        searchSettings: List<SearchSettingUiModel>,
    ): List<TextMatchUiModel>
}
