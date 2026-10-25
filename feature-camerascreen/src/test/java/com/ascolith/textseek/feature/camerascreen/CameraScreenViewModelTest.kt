package com.ascolith.textseek.feature.camerascreen

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import arrow.core.Either
import arrow.core.right
import com.ascolith.textseek.core.data.searchgroup.ISearchGroupRepository
import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.core.data.settings.IAppSettingsRepository
import com.ascolith.textseek.core.hardware.camera.CameraState
import com.ascolith.textseek.core.hardware.camera.ICameraController
import com.ascolith.textseek.core.hardware.camera.ITextImageAnalyzer
import com.ascolith.textseek.domain.feature.vibration.IVibratorController
import com.ascolith.textseek.domain.model.AppSetting
import com.ascolith.textseek.domain.model.RecognizedLine
import com.ascolith.textseek.domain.model.RecognizedText
import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.domain.model.TextRecognitionScript
import com.ascolith.textseek.domain.model.TsRect
import com.ascolith.textseek.feature.camerascreen.ui.CameraScreenViewModel
import com.ascolith.textseek.feature.camerascreen.ui.matchextractor.ITextMatchExtractor
import com.ascolith.textseek.feature.camerascreen.ui.model.ChipType
import com.ascolith.textseek.feature.camerascreen.ui.model.QuickSearchUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.ScreenIntent
import com.ascolith.textseek.feature.camerascreen.ui.model.SearchSettingUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.TsDialog
import com.ascolith.textseek.feature.camerascreen.ui.model.toSearchSettingUiModel
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraScreenViewModelTest {

    private val searchGroup = SearchGroup(
        id = SEARCH_GROUP_ID,
        name = "Groceries",
        enabled = true,
        colorArgb = GROUP_COLOR_ARGB,
    )

    private val searchSettingsFlow = MutableStateFlow<List<SearchSetting>>(emptyList())
    private val searchGroupsFlow = MutableStateFlow(listOf(searchGroup))
    private val recognizedLinesFlow = MutableStateFlow<List<RecognizedLine>>(emptyList())
    private val recordingExtractor = RecordingTextMatchExtractor()
    private val appSettingsRepository = FakeAppSettingsRepository()
    private val textImageAnalyzer = FakeTextImageAnalyzer(recognizedLinesFlow)

    private fun searchSetting(
        id: Long,
        text: String,
        caseSensitive: Boolean = false,
        color: Int? = null,
        enabled: Boolean = true,
        searchGroupId: SearchGroupId? = SEARCH_GROUP_ID,
    ) = SearchSetting(
        id = SearchSettingId(id),
        text = text,
        caseSensitive = caseSensitive,
        color = color,
        enabled = enabled,
        searchGroupId = searchGroupId,
    )

    private fun buildViewModel(savedStateHandle: SavedStateHandle) =
        CameraScreenViewModel(
            searchSettingRepository = FakeSearchSettingRepository(searchSettingsFlow),
            searchGroupRepository = FakeSearchGroupRepository(searchGroupsFlow, searchGroup),
            appSettingsRepository = appSettingsRepository,
            vibratorController = FakeVibratorController(),
            cameraController = FakeCameraController(),
            textImageAnalyzer = textImageAnalyzer,
            textMatchExtractor = recordingExtractor,
            savedStateHandle = savedStateHandle,
        )

    @Test
    fun `start quick search activates it and persists to saved state handle`() = runTest {
        val savedStateHandle = SavedStateHandle()

        buildViewModel(savedStateHandle).test(this) {
            containerHost.handleIntent(ScreenIntent.QuickSearch.Clicked)
            expectState { copy(dialog = TsDialog.QuickSearch(colorArgb = QUICK_SEARCH_DEFAULT_COLOR_ARGB)) }

            containerHost.handleIntent(ScreenIntent.QuickSearch.UpdateText(" cat "))
            expectState {
                copy(dialog = TsDialog.QuickSearch(colorArgb = QUICK_SEARCH_DEFAULT_COLOR_ARGB, text = " cat "))
            }

            containerHost.handleIntent(ScreenIntent.QuickSearch.Start)
            expectState {
                copy(
                    quickSearch = QuickSearchUiModel(
                        text = "cat",
                        caseSensitive = false,
                        colorArgb = QUICK_SEARCH_DEFAULT_COLOR_ARGB,
                    ),
                    dialog = TsDialog.None,
                )
            }

            // Re-launching the dialog pre-fills it with the active quick search.
            containerHost.handleIntent(ScreenIntent.QuickSearch.Clicked)
            expectState {
                copy(
                    dialog = TsDialog.QuickSearch(
                        colorArgb = QUICK_SEARCH_DEFAULT_COLOR_ARGB,
                        text = "cat",
                        caseSensitive = false,
                    )
                )
            }
        }

        assertEquals("cat", savedStateHandle.get<String>(KEY_TEXT))
        assertEquals(false, savedStateHandle.get<Boolean>(KEY_CASE_SENSITIVE))
        assertEquals(QUICK_SEARCH_DEFAULT_COLOR_ARGB, savedStateHandle.get<Int>(KEY_COLOR))
    }

    @Test
    fun `direct start activates quick search without showing the dialog`() = runTest {
        val savedStateHandle = SavedStateHandle()

        buildViewModel(savedStateHandle).test(this) {
            containerHost.handleIntent(
                ScreenIntent.QuickSearch.StartDirect(
                    query = " gate B12 ",
                    caseSensitive = true,
                    colorArgb = QUICK_SEARCH_COLOR_ARGB,
                )
            )
            expectState {
                copy(
                    quickSearch = QuickSearchUiModel(
                        text = "gate B12",
                        caseSensitive = true,
                        colorArgb = QUICK_SEARCH_COLOR_ARGB,
                    ),
                    dialog = TsDialog.None,
                )
            }
        }

        assertEquals("gate B12", savedStateHandle.get<String>(KEY_TEXT))
        assertEquals(true, savedStateHandle.get<Boolean>(KEY_CASE_SENSITIVE))
        assertEquals(QUICK_SEARCH_COLOR_ARGB, savedStateHandle.get<Int>(KEY_COLOR))
    }

    @Test
    fun `direct start without a color uses the default indicator color`() = runTest {
        buildViewModel(SavedStateHandle()).test(this) {
            containerHost.handleIntent(ScreenIntent.QuickSearch.StartDirect(query = "cat"))
            expectState {
                copy(
                    quickSearch = QuickSearchUiModel(
                        text = "cat",
                        caseSensitive = false,
                        colorArgb = QUICK_SEARCH_DEFAULT_COLOR_ARGB,
                    ),
                    dialog = TsDialog.None,
                )
            }
        }
    }

    @Test
    fun `direct start with a blank query opens the empty dialog instead`() = runTest {
        val savedStateHandle = SavedStateHandle()

        buildViewModel(savedStateHandle).test(this) {
            containerHost.handleIntent(ScreenIntent.QuickSearch.StartDirect(query = "   "))
            expectState { copy(dialog = TsDialog.QuickSearch(colorArgb = QUICK_SEARCH_DEFAULT_COLOR_ARGB)) }
        }

        assertNull(savedStateHandle.get<String>(KEY_TEXT))
    }

    @Test
    fun `quick search is restored from saved state handle and stop clears it`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                KEY_TEXT to "dog",
                KEY_CASE_SENSITIVE to true,
                KEY_COLOR to QUICK_SEARCH_COLOR_ARGB,
            )
        )

        buildViewModel(savedStateHandle).test(this) {
            runOnCreate()
            val restored = awaitState()
            assertEquals(
                QuickSearchUiModel(
                    text = "dog",
                    caseSensitive = true,
                    colorArgb = QUICK_SEARCH_COLOR_ARGB,
                ),
                restored.quickSearch,
            )

            awaitState() // Persisted search settings loaded.

            containerHost.handleIntent(ScreenIntent.QuickSearch.Stop)
            val stopped = awaitState()
            assertNull(stopped.quickSearch)

            cancelAndIgnoreRemainingItems()
        }

        assertNull(savedStateHandle.get<String>(KEY_TEXT))
        assertNull(savedStateHandle.get<Boolean>(KEY_CASE_SENSITIVE))
        assertNull(savedStateHandle.get<Int>(KEY_COLOR))
    }

    @Test
    fun `only the quick search term is matched while active`() = runTest {
        searchSettingsFlow.value = listOf(
            SearchSetting(
                id = SearchSettingId(10L),
                text = "cat",
                caseSensitive = false,
                color = null,
                enabled = true,
                searchGroupId = SEARCH_GROUP_ID,
            )
        )
        recognizedLinesFlow.value = listOf(
            RecognizedLine(
                listOf(
                    RecognizedText(
                        text = "cat and dog",
                        boundingBox = TsRect(0f, 0f, 1f, 1f),
                        angle = 0f,
                    )
                )
            )
        )
        val savedStateHandle = SavedStateHandle(
            mapOf(
                KEY_TEXT to "dog",
                KEY_CASE_SENSITIVE to false,
                KEY_COLOR to QUICK_SEARCH_COLOR_ARGB,
            )
        )

        buildViewModel(savedStateHandle).test(this) {
            runOnCreate()
            awaitState() // Quick search restored.
            val settingsLoaded = awaitState() // Persisted search settings loaded.
            assertEquals(1, settingsLoaded.searchSettings.count())
            advanceUntilIdle()

            cancelAndIgnoreRemainingItems()
        }

        val expectedQuickSearchSetting = QuickSearchUiModel(
            text = "dog",
            caseSensitive = false,
            colorArgb = QUICK_SEARCH_COLOR_ARGB,
        ).toSearchSettingUiModel()
        assertEquals(listOf(expectedQuickSearchSetting), recordingExtractor.lastSearchSettings)
    }

    @Test
    fun `a quick search missing its color is discarded rather than half restored`() = runTest {
        // A record an older build could have written before the color key existed.
        val savedStateHandle = SavedStateHandle(
            mapOf(
                KEY_TEXT to "dog",
                KEY_CASE_SENSITIVE to true,
            )
        )

        buildViewModel(savedStateHandle).test(this) {
            runOnCreate()
            val loaded = awaitState() // Persisted search settings loaded.
            assertNull(loaded.quickSearch)

            cancelAndIgnoreRemainingItems()
        }

        // The unusable leftovers are gone, not left to be misread on a later restore.
        assertNull(savedStateHandle.get<String>(KEY_TEXT))
        assertNull(savedStateHandle.get<Boolean>(KEY_CASE_SENSITIVE))
    }

    @Test
    fun `a quick search missing its text is discarded rather than half restored`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(KEY_COLOR to QUICK_SEARCH_COLOR_ARGB)
        )

        buildViewModel(savedStateHandle).test(this) {
            runOnCreate()
            val loaded = awaitState() // Persisted search settings loaded.
            assertNull(loaded.quickSearch)

            cancelAndIgnoreRemainingItems()
        }

        assertNull(savedStateHandle.get<Int>(KEY_COLOR))
    }

    @Test
    fun `a single group shows a chip per search setting`() = runTest {
        searchSettingsFlow.value = listOf(
            searchSetting(id = 10L, text = "cat"),
            searchSetting(id = 11L, text = "dog", enabled = false),
        )

        buildViewModel(SavedStateHandle()).test(this) {
            runOnCreate()
            val loaded = awaitState()

            assertEquals(
                listOf(ChipType.SearchSetting(SearchSettingId(10L)), ChipType.SearchSetting(SearchSettingId(11L))),
                loaded.searchChips.map { it.type },
            )
            assertEquals(listOf("cat", "dog"), loaded.searchChips.map { it.text })
            assertEquals(listOf(true, false), loaded.searchChips.map { it.isEnabled })
            // Only an enabled chip carries the match icon.
            assertEquals(listOf(true, false), loaded.searchChips.map { it.leadingIcon != null })

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `more than one group shows a chip per group instead of per setting`() = runTest {
        val secondGroup = SearchGroup(
            id = SECOND_SEARCH_GROUP_ID,
            name = "Hardware",
            enabled = false,
            colorArgb = SECOND_GROUP_COLOR_ARGB,
        )
        searchGroupsFlow.value = listOf(searchGroup, secondGroup)
        searchSettingsFlow.value = listOf(searchSetting(id = 10L, text = "cat"))

        buildViewModel(SavedStateHandle()).test(this) {
            runOnCreate()
            val loaded = awaitState()

            assertEquals(
                listOf(ChipType.SearchGroup(SEARCH_GROUP_ID), ChipType.SearchGroup(SECOND_SEARCH_GROUP_ID)),
                loaded.searchChips.map { it.type },
            )
            assertEquals(listOf("Groceries", "Hardware"), loaded.searchChips.map { it.text })
            assertEquals(listOf(true, false), loaded.searchChips.map { it.isEnabled })
            // The settings themselves are still tracked, they are just not what is shown.
            assertEquals(listOf("cat"), loaded.searchSettings.map { it.text })

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a setting without its own color inherits the group color`() = runTest {
        searchSettingsFlow.value = listOf(
            searchSetting(id = 10L, text = "inherits", color = null),
            searchSetting(id = 11L, text = "overrides", color = QUICK_SEARCH_COLOR_ARGB),
        )

        buildViewModel(SavedStateHandle()).test(this) {
            runOnCreate()
            val loaded = awaitState()

            assertEquals(
                listOf(GROUP_COLOR_ARGB, QUICK_SEARCH_COLOR_ARGB),
                loaded.searchSettings.map { it.colorArgb },
            )

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a disabled group disables every setting under it`() = runTest {
        searchGroupsFlow.value = listOf(searchGroup.copy(enabled = false))
        searchSettingsFlow.value = listOf(
            searchSetting(id = 10L, text = "cat", enabled = true),
            searchSetting(id = 11L, text = "dog", enabled = false),
        )

        buildViewModel(SavedStateHandle()).test(this) {
            runOnCreate()
            val loaded = awaitState()

            // An enabled setting in a disabled group is still off, so nothing is matched.
            assertEquals(listOf(false, false), loaded.searchSettings.map { it.enabled })

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `settings without a resolvable group are dropped instead of crashing`() = runTest {
        searchSettingsFlow.value = listOf(
            SearchSetting(
                id = SearchSettingId(10L),
                text = "in a group",
                caseSensitive = false,
                color = null,
                enabled = true,
                searchGroupId = SEARCH_GROUP_ID,
            ),
            // No group at all — the repository permits a null group id.
            SearchSetting(
                id = SearchSettingId(11L),
                text = "no group",
                caseSensitive = false,
                color = null,
                enabled = true,
                searchGroupId = null,
            ),
            // Group already removed from the group flow, as happens transiently while a
            // group delete propagates through the two independent Room queries.
            SearchSetting(
                id = SearchSettingId(12L),
                text = "deleted group",
                caseSensitive = false,
                color = null,
                enabled = true,
                searchGroupId = SearchGroupId(999L),
            ),
        )

        buildViewModel(SavedStateHandle()).test(this) {
            runOnCreate()
            val loaded = awaitState()

            assertEquals(listOf("in a group"), loaded.searchSettings.map { it.text })
            assertEquals(listOf("in a group"), loaded.searchChips.map { it.text })

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a burst of zoom changes only applies the latest value`() = runTest {
        val recordingCameraController = RecordingZoomCameraController()
        val viewModel = CameraScreenViewModel(
            searchSettingRepository = FakeSearchSettingRepository(searchSettingsFlow),
            searchGroupRepository = FakeSearchGroupRepository(searchGroupsFlow, searchGroup),
            appSettingsRepository = appSettingsRepository,
            vibratorController = FakeVibratorController(),
            cameraController = recordingCameraController,
            textImageAnalyzer = textImageAnalyzer,
            textMatchExtractor = recordingExtractor,
            savedStateHandle = SavedStateHandle(),
        )

        viewModel.test(this) {
            runOnCreate()
            awaitState() // Persisted search settings loaded.

            // A drag emits a value per frame; the intermediate ones must not reach the camera.
            containerHost.handleIntent(ScreenIntent.ZoomChanged(0.1f))
            containerHost.handleIntent(ScreenIntent.ZoomChanged(0.5f))
            containerHost.handleIntent(ScreenIntent.ZoomChanged(0.9f))
            runCurrent()

            assertEquals(listOf(0.9f), recordingCameraController.appliedZooms)

            // A later drag still gets through.
            containerHost.handleIntent(ScreenIntent.ZoomChanged(0.2f))
            runCurrent()

            assertEquals(listOf(0.9f, 0.2f), recordingCameraController.appliedZooms)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the text recognition script setting is applied to the analyzer, including changes`() = runTest {
        appSettingsRepository.values.value =
            mapOf(AppSetting.TextRecognitionScript.key to TextRecognitionScript.JAPANESE.name)

        buildViewModel(SavedStateHandle()).test(this) {
            runOnCreate()
            awaitState() // Persisted search settings loaded.
            runCurrent()

            assertEquals(TextRecognitionScript.JAPANESE, textImageAnalyzer.script)

            appSettingsRepository.values.value =
                mapOf(AppSetting.TextRecognitionScript.key to TextRecognitionScript.KOREAN.name)
            runCurrent()

            assertEquals(TextRecognitionScript.KOREAN, textImageAnalyzer.script)

            cancelAndIgnoreRemainingItems()
        }
    }

    private class RecordingTextMatchExtractor : ITextMatchExtractor {
        var lastSearchSettings: List<SearchSettingUiModel>? = null

        override fun extract(
            recognizedLines: List<RecognizedLine>,
            searchSettings: List<SearchSettingUiModel>,
        ): List<TextMatchUiModel> {
            lastSearchSettings = searchSettings
            return emptyList()
        }
    }

    private class FakeSearchSettingRepository(
        override val searchSettings: Flow<List<SearchSetting>>,
    ) : ISearchSettingRepository {
        override suspend fun create(
            text: String,
            caseSensitive: Boolean,
            color: Int?,
            enabled: Boolean,
            searchGroupId: SearchGroupId?,
        ): Long = 0L

        override suspend fun get(id: SearchSettingId): SearchSetting? = null
        override suspend fun getInGroup(groupId: SearchGroupId): Flow<List<SearchSetting>> = searchSettings
        override suspend fun update(searchSetting: SearchSetting) = Unit
        override suspend fun update(id: SearchSettingId, text: String) = Unit
        override suspend fun delete(ids: Set<SearchSettingId>) = Unit
        override suspend fun delete(id: SearchSettingId) = Unit
    }

    private class FakeSearchGroupRepository(
        override val searchGroups: Flow<List<SearchGroup>>,
        private val searchGroup: SearchGroup,
    ) : ISearchGroupRepository {
        override suspend fun create(name: String, enabled: Boolean, colorArgb: Int): SearchGroupId =
            searchGroup.id

        override suspend fun get(id: SearchGroupId): SearchGroup? = searchGroup
        override suspend fun setGroupEnabled(id: SearchGroupId, enabled: Boolean) = Unit
        override suspend fun update(searchGroup: SearchGroup) = Unit
        override suspend fun deleteGroupAndChildren(id: SearchGroupId) = Unit
    }

    private class FakeVibratorController : IVibratorController {
        override fun vibrate(effect: IVibratorController.Effect, throttleWindow: Duration) = Unit
    }

    private class FakeCameraController : ICameraController {
        override val cameraStateFlow: StateFlow<CameraState> =
            MutableStateFlow(CameraState(zoomLevel = 0f))

        override suspend fun bindToCamera(
            lifecycleOwner: LifecycleOwner,
            imageAnalyzer: ImageAnalysis.Analyzer,
        ) = Unit

        override suspend fun zoom(zoomLevel: Float) = Unit
        override suspend fun setTorchEnabled(enabled: Boolean) = Unit
    }

    private class RecordingZoomCameraController : ICameraController {
        private val _appliedZooms = mutableListOf<Float>()
        val appliedZooms: List<Float> get() = _appliedZooms

        override val cameraStateFlow: StateFlow<CameraState> =
            MutableStateFlow(CameraState(zoomLevel = 0f))

        override suspend fun bindToCamera(
            lifecycleOwner: LifecycleOwner,
            imageAnalyzer: ImageAnalysis.Analyzer,
        ) = awaitCancellation()

        override suspend fun zoom(zoomLevel: Float) {
            _appliedZooms += zoomLevel
        }

        override suspend fun setTorchEnabled(enabled: Boolean) = Unit
    }

    private class FakeTextImageAnalyzer(
        override val recognizedLinesFlow: StateFlow<List<RecognizedLine>>,
    ) : ITextImageAnalyzer {
        override var script: TextRecognitionScript = TextRecognitionScript.LATIN
        override fun analyze(image: ImageProxy) = Unit
    }

    private class FakeAppSettingsRepository : IAppSettingsRepository {
        val values = MutableStateFlow<Map<String, String>>(emptyMap())

        override fun <T> observe(setting: AppSetting<T>): Flow<T> =
            values.map { it[setting.key]?.let(setting::decode) ?: setting.defaultValue }.distinctUntilChanged()

        override suspend fun <T> set(setting: AppSetting<T>, value: T): Either<Throwable, Unit> {
            values.update { it + (setting.key to setting.encode(value)) }
            return Unit.right()
        }

        override suspend fun exportAll(): Map<String, String> = values.value

        override suspend fun importAll(values: Map<String, String>): Either<Throwable, Unit> {
            this.values.update { it + values }
            return Unit.right()
        }
    }

    companion object {
        private val SEARCH_GROUP_ID = SearchGroupId(1L)
        private val SECOND_SEARCH_GROUP_ID = SearchGroupId(2L)
        private const val GROUP_COLOR_ARGB = 0xFFFF0000.toInt()
        private const val SECOND_GROUP_COLOR_ARGB = 0xFF0000FF.toInt()
        private const val QUICK_SEARCH_DEFAULT_COLOR_ARGB = SearchGroup.DEFAULT_COLOR_ARGB
        private const val QUICK_SEARCH_COLOR_ARGB = 0xFF00FF00.toInt()
        private const val KEY_TEXT = "quick_search_text"
        private const val KEY_CASE_SENSITIVE = "quick_search_case_sensitive"
        private const val KEY_COLOR = "quick_search_color"
    }
}
