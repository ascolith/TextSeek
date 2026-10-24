package com.ascolith.textseek.feature.settings

import android.net.FakeUri
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import android.net.Uri
import com.ascolith.textseek.core.data.backup.IBackupRepository
import com.ascolith.textseek.core.data.settings.IAppSettingsRepository
import com.ascolith.textseek.domain.feature.version.IAppVersionProvider
import com.ascolith.textseek.domain.model.AppSetting
import com.ascolith.textseek.domain.model.TextRecognitionScript
import com.ascolith.textseek.feature.settings.ui.SettingsViewModel
import com.ascolith.textseek.feature.settings.ui.model.ScreenIntent
import com.ascolith.textseek.feature.settings.ui.model.SideEffect
import com.ascolith.textseek.feature.settings.ui.model.TsDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val backupRepository = FakeBackupRepository()
    private val appSettingsRepository = FakeAppSettingsRepository()

    private fun buildViewModel() = SettingsViewModel(
        backupRepository = backupRepository,
        appSettingsRepository = appSettingsRepository,
        appVersionProvider = FakeAppVersionProvider(),
    )

    @Test
    fun `a successful export hands the share sheet the exported uri`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.ExportSettings)

            assertEquals(SideEffect.ShareExport(EXPORT_URI), awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a failed export is reported rather than silently doing nothing`() = runTest {
        backupRepository.exportFailure = IllegalStateException("no space left")

        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.ExportSettings)

            assertEquals(SideEffect.ExportFailed, awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `importing reads the selected uri and reports success`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.ImportFileSelected(IMPORT_URI))

            assertEquals(SideEffect.ImportResult(success = true), awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf(IMPORT_URI), backupRepository.imported)
    }

    @Test
    fun `an unreadable or malformed backup is reported rather than crashing`() = runTest {
        // Covers both halves: the file the user chose may be gone by the time it is read,
        // or may not be a backup at all.
        backupRepository.importFailure = IllegalArgumentException("not a backup")

        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.ImportFileSelected(IMPORT_URI))

            assertEquals(SideEffect.ImportResult(success = false), awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `importing asks for confirmation before opening the file picker`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.ImportSettings)
            assertEquals(TsDialog.ImportConfirmation, awaitState().dialog)

            containerHost.handleIntent(ScreenIntent.ConfirmImport)
            assertEquals(TsDialog.None, awaitState().dialog)
            assertEquals(SideEffect.LaunchFilePicker, awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `dismissing the confirmation does not open the file picker`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.ImportSettings)
            awaitState()

            containerHost.handleIntent(ScreenIntent.DismissDialog)
            assertEquals(TsDialog.None, awaitState().dialog)

            expectNoItems()

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `reporting an issue navigates to the issue reporter`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.ReportIssue)

            assertEquals(SideEffect.NavigateToReportIssue, awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `opening acknowledgements navigates to the acknowledgements screen`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.OpenAcknowledgements)

            assertEquals(SideEffect.NavigateToAcknowledgements, awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `opening the privacy policy opens the privacy policy link`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.OpenPrivacyPolicy)

            assertEquals(SideEffect.OpenPrivacyPolicy, awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `opening the license opens the license link`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.OpenLicense)

            assertEquals(SideEffect.OpenLicense, awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the version shown comes from the version provider`() {
        // Seeded into the initial state rather than reduced, so there is no emission to
        // await - read the container's state directly.
        val state = buildViewModel().container.stateFlow.value

        assertEquals(VERSION_NAME, state.versionName)
        assertEquals(VERSION_CODE, state.versionCode)
    }

    @Test
    fun `the global default indicator color shown comes from the settings repository`() = runTest {
        appSettingsRepository.set(AppSetting.GlobalDefaultIndicatorColor, BLUE)

        buildViewModel().test(this) {
            runOnCreate()

            assertEquals(BLUE, awaitState().globalDefaultIndicatorColorArgb)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `choosing and saving a global default indicator color stores it and shows it`() = runTest {
        buildViewModel().test(this) {
            runOnCreate()

            containerHost.handleIntent(ScreenIntent.ChooseGlobalDefaultIndicatorColor)
            assertEquals(TsDialog.GlobalDefaultIndicatorColorPicker(), awaitState().dialog)

            containerHost.handleIntent(ScreenIntent.GlobalDefaultIndicatorColorChanged(BLUE))
            assertEquals(TsDialog.GlobalDefaultIndicatorColorPicker(selectedColorArgb = BLUE), awaitState().dialog)

            containerHost.handleIntent(ScreenIntent.SaveGlobalDefaultIndicatorColor)
            assertEquals(TsDialog.None, awaitState().dialog)
            assertEquals(BLUE, awaitState().globalDefaultIndicatorColorArgb)

            cancelAndIgnoreRemainingItems()
        }

        assertEquals("#FF1B4D9B", appSettingsRepository.stored["global_default_indicator_color"])
    }

    @Test
    fun `dismissing the color picker does not store the color`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.ChooseGlobalDefaultIndicatorColor)
            awaitState()
            containerHost.handleIntent(ScreenIntent.GlobalDefaultIndicatorColorChanged(BLUE))
            awaitState()

            containerHost.handleIntent(ScreenIntent.DismissDialog)
            assertEquals(TsDialog.None, awaitState().dialog)

            cancelAndIgnoreRemainingItems()
        }

        assertNull(appSettingsRepository.stored["global_default_indicator_color"])
    }

    @Test
    fun `saving without choosing a color does not store anything`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.ChooseGlobalDefaultIndicatorColor)
            awaitState()

            containerHost.handleIntent(ScreenIntent.SaveGlobalDefaultIndicatorColor)
            assertEquals(TsDialog.None, awaitState().dialog)

            cancelAndIgnoreRemainingItems()
        }

        assertNull(appSettingsRepository.stored["global_default_indicator_color"])
    }

    @Test
    fun `the text recognition script shown comes from the settings repository`() = runTest {
        appSettingsRepository.set(AppSetting.TextRecognitionScript, TextRecognitionScript.KOREAN)

        buildViewModel().test(this) {
            runOnCreate()

            assertEquals(TextRecognitionScript.KOREAN, awaitState().textRecognitionScript)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `selecting a text recognition script stores it, shows it, and closes the picker`() = runTest {
        buildViewModel().test(this) {
            runOnCreate()

            containerHost.handleIntent(ScreenIntent.ChooseTextRecognitionScript)
            assertEquals(TsDialog.TextRecognitionScriptPicker, awaitState().dialog)

            containerHost.handleIntent(ScreenIntent.SelectTextRecognitionScript(TextRecognitionScript.DEVANAGARI))
            assertEquals(TsDialog.None, awaitState().dialog)
            assertEquals(TextRecognitionScript.DEVANAGARI, awaitState().textRecognitionScript)

            cancelAndIgnoreRemainingItems()
        }

        assertEquals("DEVANAGARI", appSettingsRepository.stored["text_recognition_script"])
    }

    @Test
    fun `dismissing the text recognition script picker does not store anything`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.ChooseTextRecognitionScript)
            awaitState()

            containerHost.handleIntent(ScreenIntent.DismissDialog)
            assertEquals(TsDialog.None, awaitState().dialog)

            cancelAndIgnoreRemainingItems()
        }

        assertNull(appSettingsRepository.stored["text_recognition_script"])
    }

    @Test
    fun `opening the debug menu navigates to it`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.OpenDebugMenu)

            assertEquals(SideEffect.NavigateToDebugMenu, awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    private class FakeAppSettingsRepository : IAppSettingsRepository {
        private val values = MutableStateFlow<Map<String, String>>(emptyMap())
        val stored: Map<String, String> get() = values.value

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

    private class FakeBackupRepository : IBackupRepository {
        val imported = mutableListOf<Uri>()
        var exportFailure: Throwable? = null
        var importFailure: Throwable? = null

        override suspend fun exportToUri(): Either<Throwable, Uri> =
            exportFailure?.left() ?: EXPORT_URI.right()

        override suspend fun importFromUri(uri: Uri): Either<Throwable, Unit> {
            importFailure?.let { return it.left() }
            imported += uri
            return Unit.right()
        }
    }

    private class FakeAppVersionProvider : IAppVersionProvider {
        override val versionName: String = VERSION_NAME
        override val versionCode: Long = VERSION_CODE
    }

    private companion object {
        val EXPORT_URI: Uri = FakeUri("content://test.fileprovider/cache/backup.json")
        val IMPORT_URI: Uri = FakeUri("content://documents/picked.json")
        const val VERSION_NAME = "1.2.3"
        const val VERSION_CODE = 42L
        val BLUE = 0xFF1B4D9B.toInt()
    }
}
