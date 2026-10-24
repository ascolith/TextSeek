package com.ascolith.textseek.feature.camerascreen.ui

import androidx.lifecycle.SavedStateHandle
import com.ascolith.textseek.feature.camerascreen.ui.model.QuickSearchUiModel

/**
 * Persists the active quick search in [SavedStateHandle] so it survives
 * configuration changes and process death.
 */
class QuickSearchSavedState(private val savedStateHandle: SavedStateHandle) {

    /** The stored search, or null if there is none to restore. */
    fun restore(): QuickSearchUiModel? {
        val text = savedStateHandle.get<String>(KEY_TEXT)
        val colorArgb = savedStateHandle.get<Int>(KEY_COLOR)
        if (text == null || colorArgb == null) {
            clear()
            return null
        }
        return QuickSearchUiModel(
            text = text,
            caseSensitive = savedStateHandle[KEY_CASE_SENSITIVE] ?: false,
            colorArgb = colorArgb,
        )
    }

    fun save(quickSearch: QuickSearchUiModel) {
        savedStateHandle[KEY_TEXT] = quickSearch.text
        savedStateHandle[KEY_CASE_SENSITIVE] = quickSearch.caseSensitive
        savedStateHandle[KEY_COLOR] = quickSearch.colorArgb
    }

    fun clear() {
        savedStateHandle.remove<String>(KEY_TEXT)
        savedStateHandle.remove<Boolean>(KEY_CASE_SENSITIVE)
        savedStateHandle.remove<Int>(KEY_COLOR)
    }

    companion object {
        private const val KEY_TEXT = "quick_search_text"
        private const val KEY_CASE_SENSITIVE = "quick_search_case_sensitive"
        private const val KEY_COLOR = "quick_search_color"
    }
}
