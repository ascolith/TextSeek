package com.ascolith.textseek.feature.camerascreen.ui.model

data class UiState(
    val transientScreen: TransientScreen = TransientScreen.None,
    val cameraState: CameraStateUiModel = CameraStateUiModel(),
    val searchChips: List<Chip> = emptyList(),
    val searchSettings: List<SearchSettingUiModel> = emptyList(),
    val quickSearch: QuickSearchUiModel? = null,
    val dialog: TsDialog = TsDialog.None,
)
