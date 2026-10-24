package com.ascolith.textseek.feature.camerascreen.ui.matchextractor

import com.ascolith.textseek.domain.model.RecognizedText
import com.ascolith.textseek.feature.camerascreen.ui.model.SearchSettingUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchFactory
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchUiModel
import javax.inject.Inject

class TextMatchExtractor @Inject constructor() : ITextMatchExtractor {

    override fun extract(
        recognizedTexts: List<RecognizedText>,
        searchSettings: List<SearchSettingUiModel>,
    ): List<TextMatchUiModel> =
        searchSettings
            .filter { it.enabled && it.text.isNotEmpty() }
            .flatMap { search ->
                recognizedTexts
                    .filter { it.text.contains(search.text, ignoreCase = !search.caseSensitive) }
                    .map { TextMatchFactory.createTextMatch(it, search.colorArgb) }
            }
}
