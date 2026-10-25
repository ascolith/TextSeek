package com.ascolith.textseek.feature.camerascreen.ui.matchextractor

import com.ascolith.textseek.domain.model.RecognizedLine
import com.ascolith.textseek.feature.camerascreen.ui.model.SearchSettingUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchUiModel
import javax.inject.Inject

class TextMatchExtractor @Inject constructor() : ITextMatchExtractor {

    // Settings change far less often than frames arrive, so keep the terms of the last settings.
    @Volatile
    private var cachedTerms: Pair<List<SearchSettingUiModel>, List<SearchTerm>>? = null

    override fun extract(
        recognizedLines: List<RecognizedLine>,
        searchSettings: List<SearchSettingUiModel>,
    ): List<TextMatchUiModel> {
        val terms = termsFor(searchSettings)
        if (terms.isEmpty()) return emptyList()

        val lines = recognizedLines.map(::SearchableLine)
        return terms.flatMap { term -> lines.flatMap { it.findMatches(term) } }
    }

    private fun termsFor(searchSettings: List<SearchSettingUiModel>): List<SearchTerm> =
        cachedTerms?.takeIf { (settings, _) -> settings == searchSettings }?.second
            ?: searchSettings.mapNotNull(SearchTerm::from).also { cachedTerms = searchSettings to it }
}
