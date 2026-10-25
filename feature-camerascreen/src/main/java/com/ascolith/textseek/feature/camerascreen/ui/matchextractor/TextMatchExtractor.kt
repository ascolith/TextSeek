package com.ascolith.textseek.feature.camerascreen.ui.matchextractor

import com.ascolith.textseek.domain.model.RecognizedLine
import com.ascolith.textseek.feature.camerascreen.ui.model.SearchSettingUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchUiModel
import javax.inject.Inject

class TextMatchExtractor @Inject constructor(
    private val wordSeparator: IWordSeparator,
) : ITextMatchExtractor {

    // Settings change far less often than frames arrive, so keep the terms of the last settings.
    @Volatile
    private var cachedTerms: Pair<List<SearchSettingUiModel>, List<SearchTerm>>? = null

    override fun extract(
        recognizedLines: List<RecognizedLine>,
        searchSettings: List<SearchSettingUiModel>,
    ): List<TextMatchUiModel> {
        val terms = termsFor(searchSettings)
        if (terms.isEmpty()) return emptyList()

        val lines = recognizedLines.map { SearchableLine(it, wordSeparator) }
        return terms.flatMap { term -> lines.flatMap { it.findMatches(term) } }
    }

    private fun termsFor(searchSettings: List<SearchSettingUiModel>): List<SearchTerm> =
        cachedTerms?.takeIf { (settings, _) -> settings == searchSettings }?.second
            ?: searchSettings.mapNotNull { SearchTerm.from(it, wordSeparator) }.also { cachedTerms = searchSettings to it }
}
