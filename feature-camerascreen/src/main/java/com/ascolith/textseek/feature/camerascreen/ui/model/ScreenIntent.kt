package com.ascolith.textseek.feature.camerascreen.ui.model

sealed interface ScreenIntent {
    data class SearchChipClicked(val chip: Chip) : ScreenIntent
    data class ZoomChanged(val zoomLevel: Float) : ScreenIntent
    data object TorchToggled : ScreenIntent

    sealed interface QuickSearch : ScreenIntent {
        data object Clicked : QuickSearch
        data class UpdateText(val text: String) : QuickSearch
        data object ToggleCaseSensitive : QuickSearch
        data object ShowColorPicker : QuickSearch
        data class UpdateColorPickerColor(val colorArgb: Int) : QuickSearch
        data object SaveColor : QuickSearch
        data object HideColorPicker : QuickSearch
        data object Dismiss : QuickSearch
        data object Start : QuickSearch
        data class StartDirect(
            val query: String,
            val caseSensitive: Boolean = false,
            val colorArgb: Int? = null,
        ) : QuickSearch
        data object Stop : QuickSearch
    }
}
