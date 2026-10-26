package com.ascolith.textseek.feature.camerascreen.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ascolith.textseek.core.data.searchgroup.ISearchGroupRepository
import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.core.data.settings.IAppSettingsRepository
import com.ascolith.textseek.core.hardware.camera.ICameraController
import com.ascolith.textseek.core.hardware.camera.ITextImageAnalyzer
import com.ascolith.textseek.domain.feature.vibration.IVibratorController
import com.ascolith.textseek.domain.model.AppSetting
import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.feature.camerascreen.ui.matchextractor.ITextMatchExtractor
import com.ascolith.textseek.feature.camerascreen.ui.model.Chip
import com.ascolith.textseek.feature.camerascreen.ui.model.ChipType
import com.ascolith.textseek.feature.camerascreen.ui.model.QuickSearchUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.ScreenIntent
import com.ascolith.textseek.feature.camerascreen.ui.model.SearchGroupUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.SearchSettingUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.SideEffect
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.TransientScreen
import com.ascolith.textseek.feature.camerascreen.ui.model.UiState
import com.ascolith.textseek.feature.camerascreen.ui.model.TsDialog
import com.ascolith.textseek.feature.camerascreen.ui.model.toSearchSettingUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.toUiModel
import com.ascolith.textseek.resources.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container

@OptIn(OrbitExperimental::class)
@HiltViewModel
class CameraScreenViewModel @Inject constructor(
    private val searchSettingRepository: ISearchSettingRepository,
    private val searchGroupRepository: ISearchGroupRepository,
    private val appSettingsRepository: IAppSettingsRepository,
    private val vibratorController: IVibratorController,
    private val cameraController: ICameraController,
    private val textImageAnalyzer: ITextImageAnalyzer,
    private val textMatchExtractor: ITextMatchExtractor,
    savedStateHandle: SavedStateHandle,
) : ContainerHost<UiState, SideEffect>, ViewModel() {

    private val quickSearchSavedState = QuickSearchSavedState(savedStateHandle)

    private val _textMatches = MutableStateFlow<List<TextMatchUiModel>>(emptyList())
    val textMatches: StateFlow<List<TextMatchUiModel>> = _textMatches.asStateFlow()

    /**
     * Latest requested zoom, held rather than applied directly since a slider drag produces a
     * value per frame while [ICameraController.zoom] awaits the camera.
     */
    private val zoomRequests = MutableStateFlow<Float?>(null)

    override val container = container<UiState, SideEffect>(UiState(transientScreen = TransientScreen.Loading)) {
        intent {
            restoreQuickSearch()
            coroutineScope {
                launch { observeSearchSettings() }
                launch { observeCameraStateFlow() }
                launch { observeRecognizedTextsFlow() }
                launch { observeTextRecognitionScript() }
                launch { observeZoomRequests() }
            }
        }
    }

    fun handleIntent(intent: ScreenIntent) {
        when (intent) {
            is ScreenIntent.SearchChipClicked -> handleSearchChipClicked(intent.chip)
            is ScreenIntent.ZoomChanged -> handleZoomChanged(intent.zoomLevel)
            ScreenIntent.TorchToggled -> handleTorchToggled()
            is ScreenIntent.QuickSearch -> handleQuickSearchIntent(intent)
        }
    }

    private fun handleQuickSearchIntent(intent: ScreenIntent.QuickSearch) {
        when (intent) {
            ScreenIntent.QuickSearch.Clicked -> handleQuickSearchClicked()
            is ScreenIntent.QuickSearch.UpdateText -> updateQuickSearchDialog { it.copy(text = intent.text) }
            ScreenIntent.QuickSearch.ToggleCaseSensitive ->
                updateQuickSearchDialog { it.copy(caseSensitive = !it.caseSensitive) }
            ScreenIntent.QuickSearch.ShowColorPicker ->
                updateQuickSearchDialog { it.copy(isColorPickerVisible = true, colorPickerSelectedColorArgb = null) }
            is ScreenIntent.QuickSearch.UpdateColorPickerColor ->
                updateQuickSearchDialog { it.copy(colorPickerSelectedColorArgb = intent.colorArgb) }
            ScreenIntent.QuickSearch.SaveColor -> updateQuickSearchDialog {
                it.copy(
                    colorArgb = it.colorPickerSelectedColorArgb ?: it.colorArgb,
                    isColorPickerVisible = false,
                )
            }
            ScreenIntent.QuickSearch.HideColorPicker ->
                updateQuickSearchDialog { it.copy(isColorPickerVisible = false) }
            ScreenIntent.QuickSearch.Dismiss -> handleDismissQuickSearchDialog()
            ScreenIntent.QuickSearch.Start -> handleStartQuickSearch()
            is ScreenIntent.QuickSearch.StartDirect ->
                handleStartQuickSearchDirect(intent.query, intent.caseSensitive, intent.colorArgb)
            ScreenIntent.QuickSearch.Stop -> handleStopQuickSearch()
        }
    }

    /**
     * Binds the camera for as long as the caller's scope is alive; cancelling the caller
     * releases the camera.
     */
    suspend fun bindToCamera(lifecycleOwner: LifecycleOwner) {
        cameraController.bindToCamera(lifecycleOwner, textImageAnalyzer)
    }

    private fun handleZoomChanged(zoomLevel: Float) {
        zoomRequests.value = zoomLevel
    }

    private fun handleTorchToggled() = intent {
        val currentlyEnabled = state.cameraState.torchEnabled
        cameraController.setTorchEnabled(!currentlyEnabled)
    }

    private fun handleQuickSearchClicked() = intent {
        val activeQuickSearch = state.quickSearch
        val dialog = if (activeQuickSearch != null) {
            TsDialog.QuickSearch(
                colorArgb = activeQuickSearch.colorArgb,
                text = activeQuickSearch.text,
                caseSensitive = activeQuickSearch.caseSensitive,
            )
        } else {
            TsDialog.QuickSearch(colorArgb = SearchGroup.DEFAULT_COLOR_ARGB)
        }
        reduce { state.copy(dialog = dialog) }
    }

    private fun updateQuickSearchDialog(
        update: (TsDialog.QuickSearch) -> TsDialog.QuickSearch,
    ) = intent {
        val dialog = state.dialog as? TsDialog.QuickSearch ?: return@intent
        reduce { state.copy(dialog = update(dialog)) }
    }

    private fun handleDismissQuickSearchDialog() = intent {
        reduce { state.copy(dialog = TsDialog.None) }
    }

    private fun handleStartQuickSearch() = intent {
        val dialog = state.dialog as? TsDialog.QuickSearch ?: return@intent
        val quickSearch = QuickSearchUiModel(
            text = dialog.text.trim(),
            caseSensitive = dialog.caseSensitive,
            colorArgb = dialog.colorArgb,
        )
        quickSearchSavedState.save(quickSearch)
        reduce { state.copy(quickSearch = quickSearch, dialog = TsDialog.None) }
    }

    private fun handleStartQuickSearchDirect(
        query: String,
        caseSensitive: Boolean,
        colorArgb: Int?,
    ) = intent {
        val text = query.trim()
        if (text.isEmpty()) {
            handleQuickSearchClicked()
            return@intent
        }
        val quickSearch = QuickSearchUiModel(
            text = text,
            caseSensitive = caseSensitive,
            colorArgb = colorArgb ?: SearchGroup.DEFAULT_COLOR_ARGB,
        )
        quickSearchSavedState.save(quickSearch)
        reduce { state.copy(quickSearch = quickSearch, dialog = TsDialog.None) }
    }

    private fun handleStopQuickSearch() = intent {
        quickSearchSavedState.clear()
        reduce { state.copy(quickSearch = null) }
    }

    private suspend fun restoreQuickSearch() = subIntent {
        val quickSearch = quickSearchSavedState.restore() ?: return@subIntent
        reduce { state.copy(quickSearch = quickSearch) }
    }

    private fun handleSearchChipClicked(chip: Chip) = intent {
        val sideEffect = when (chip.type) {
            is ChipType.SearchSetting -> SideEffect.NavigateToSearchSettingEdit(chip.type.id)
            is ChipType.SearchGroup -> SideEffect.NavigateToSearchGroupEdit(chip.type.id)
        }
        postSideEffect(sideEffect)
    }

    private suspend fun observeSearchSettings() = subIntent {
        repeatOnSubscription {
            combine(
                searchSettingRepository.searchSettings,
                searchGroupRepository.searchGroups
            ) { searchSettings, searchGroups ->
                val searchSettingUiModels = searchSettings.toUiModel(
                    searchGroupMap = searchGroups.associateBy { it.id }
                )

                val chips = if (searchGroups.size > 1) {
                    searchGroups
                        .map { it.toUiModel() }
                        .mapToGroupChips()
                } else {
                    searchSettingUiModels.mapToSettingChips()
                }

                searchSettingUiModels to chips
            }.distinctUntilChanged()
            .collect { (searchSettings: List<SearchSettingUiModel>, chips: List<Chip>) ->
                reduce {
                    state.copy(
                        transientScreen = TransientScreen.None,
                        searchSettings = searchSettings,
                        searchChips = chips,
                    )
                }
            }
        }
    }

    private suspend fun observeZoomRequests() = subIntent {
        repeatOnSubscription {
            zoomRequests
                .filterNotNull()
                .collectLatest { zoomLevel ->
                    cameraController.zoom(zoomLevel)
                }
        }
    }

    private suspend fun observeCameraStateFlow() = subIntent {
        repeatOnSubscription {
            cameraController.cameraStateFlow
                .collect { cameraState ->
                    reduce {
                        state.copy(
                            cameraState = cameraState.toUiModel(),
                            transientScreen = if (cameraState.surfaceRequest != null) {
                                TransientScreen.None
                            } else {
                                state.transientScreen
                            },
                        )
                    }
                }
        }
    }

    private suspend fun observeTextRecognitionScript() = subIntent {
        repeatOnSubscription {
            appSettingsRepository.observe(AppSetting.TextRecognitionScript)
                .collect { textImageAnalyzer.script = it }
        }
    }

    private suspend fun observeRecognizedTextsFlow() = subIntent {
        repeatOnSubscription {
            textImageAnalyzer.recognizedLinesFlow
                .collectLatest { recognizedLines ->
                    val activeSettings = state.quickSearch
                        ?.let { listOf(it.toSearchSettingUiModel()) }
                        ?: state.searchSettings
                    val textMatches = textMatchExtractor.extract(recognizedLines, activeSettings)
                    if (textMatches.isNotEmpty()) {
                        vibratorController.vibrate(
                            effect = IVibratorController.Effect.DoubleClick,
                            throttleWindow = VIBRATION_THROTTLE_WINDOW,
                        )
                    }

                    _textMatches.value = textMatches
                }
        }
    }

    private fun List<SearchSetting>.toUiModel(
        searchGroupMap: Map<SearchGroupId, SearchGroup>,
    ): List<SearchSettingUiModel> =
        mapNotNull { setting ->
            val searchGroup = setting.searchGroupId?.let(searchGroupMap::get)
                ?: return@mapNotNull null
            setting.toUiModel(
                isGroupEnabled = searchGroup.enabled,
                groupColorArgb = searchGroup.colorArgb,
            )
        }

    private fun List<SearchGroupUiModel>.mapToGroupChips(): List<Chip> =
        map { group ->
            Chip(
                type = ChipType.SearchGroup(group.id),
                text = group.name,
                isEnabled = group.enabled,
                contentDescription = R.string.content_description_search_group_name,
            )
        }

    private fun List<SearchSettingUiModel>.mapToSettingChips(): List<Chip> =
        map { setting ->
            Chip(
                type = ChipType.SearchSetting(setting.id),
                text = setting.text,
                isEnabled = setting.enabled,
                contentDescription = R.string.content_description_search_setting,
                leadingIcon = if (setting.enabled) {
                    R.drawable.ic_text_match_outline
                } else {
                    null
                },
                iconColorArgb = setting.colorArgb,
            )
        }

    companion object {
        private val VIBRATION_THROTTLE_WINDOW = 500.milliseconds
    }
}
