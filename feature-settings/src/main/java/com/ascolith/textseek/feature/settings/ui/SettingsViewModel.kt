package com.ascolith.textseek.feature.settings.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.ascolith.textseek.core.data.backup.IBackupRepository
import com.ascolith.textseek.core.data.settings.IAppSettingsRepository
import com.ascolith.textseek.domain.feature.version.IAppVersionProvider
import com.ascolith.textseek.domain.model.AppSetting
import com.ascolith.textseek.domain.model.TextRecognitionScript
import com.ascolith.textseek.feature.settings.ui.model.ScreenIntent
import com.ascolith.textseek.feature.settings.ui.model.SideEffect
import com.ascolith.textseek.feature.settings.ui.model.UiState
import com.ascolith.textseek.feature.settings.ui.model.TsDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: IBackupRepository,
    private val appSettingsRepository: IAppSettingsRepository,
    appVersionProvider: IAppVersionProvider,
) : ContainerHost<UiState, SideEffect>, ViewModel() {

    override val container = container<UiState, SideEffect>(
        UiState(
            versionName = appVersionProvider.versionName,
            versionCode = appVersionProvider.versionCode,
        ),
    ) {
        coroutineScope {
            launch { observeGlobalDefaultIndicatorColor() }
            launch { observeTextRecognitionScript() }
        }
    }

    fun handleIntent(intent: ScreenIntent) {
        when (intent) {
            ScreenIntent.ExportSettings -> exportSettings()
            ScreenIntent.ImportSettings -> showImportConfirmation()
            ScreenIntent.ConfirmImport -> confirmImport()
            ScreenIntent.DismissDialog -> dismissDialog()
            is ScreenIntent.ImportFileSelected -> importFrom(intent.uri)
            ScreenIntent.ChooseGlobalDefaultIndicatorColor -> showGlobalDefaultIndicatorColorPicker()
            is ScreenIntent.GlobalDefaultIndicatorColorChanged ->
                updateSelectedGlobalDefaultIndicatorColor(intent.colorArgb)

            ScreenIntent.SaveGlobalDefaultIndicatorColor -> saveGlobalDefaultIndicatorColor()
            ScreenIntent.ChooseTextRecognitionScript -> showTextRecognitionScriptPicker()
            is ScreenIntent.SelectTextRecognitionScript -> saveTextRecognitionScript(intent.script)
            ScreenIntent.ReportIssue -> intent { postSideEffect(SideEffect.NavigateToReportIssue) }
            ScreenIntent.OpenAcknowledgements -> intent { postSideEffect(SideEffect.NavigateToAcknowledgements) }
            ScreenIntent.OpenPrivacyPolicy -> intent { postSideEffect(SideEffect.OpenPrivacyPolicy) }
            ScreenIntent.OpenLicense -> intent { postSideEffect(SideEffect.OpenLicense) }
            ScreenIntent.OpenSourceCode -> intent { postSideEffect(SideEffect.OpenSourceCode) }
            ScreenIntent.OpenDebugMenu -> intent { postSideEffect(SideEffect.NavigateToDebugMenu) }
        }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun observeGlobalDefaultIndicatorColor() = subIntent {
        repeatOnSubscription {
            appSettingsRepository.observe(AppSetting.GlobalDefaultIndicatorColor)
                .collect { colorArgb -> reduce { state.copy(globalDefaultIndicatorColorArgb = colorArgb) } }
        }
    }

    private fun showGlobalDefaultIndicatorColorPicker() = intent {
        reduce { state.copy(dialog = TsDialog.GlobalDefaultIndicatorColorPicker()) }
    }

    private fun updateSelectedGlobalDefaultIndicatorColor(colorArgb: Int) = intent {
        val dialog = state.dialog as? TsDialog.GlobalDefaultIndicatorColorPicker ?: return@intent
        reduce { state.copy(dialog = dialog.copy(selectedColorArgb = colorArgb)) }
    }

    private fun saveGlobalDefaultIndicatorColor() = intent {
        val dialog = state.dialog as? TsDialog.GlobalDefaultIndicatorColorPicker ?: return@intent
        reduce { state.copy(dialog = TsDialog.None) }
        val colorArgb = dialog.selectedColorArgb ?: return@intent
        appSettingsRepository.set(AppSetting.GlobalDefaultIndicatorColor, colorArgb)
            .onLeft { Logger.e(it) { "Failed to save the global default indicator color" } }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun observeTextRecognitionScript() = subIntent {
        repeatOnSubscription {
            appSettingsRepository.observe(AppSetting.TextRecognitionScript)
                .collect { script -> reduce { state.copy(textRecognitionScript = script) } }
        }
    }

    private fun showTextRecognitionScriptPicker() = intent {
        reduce { state.copy(dialog = TsDialog.TextRecognitionScriptPicker) }
    }

    private fun saveTextRecognitionScript(script: TextRecognitionScript) = intent {
        reduce { state.copy(dialog = TsDialog.None) }
        appSettingsRepository.set(AppSetting.TextRecognitionScript, script)
            .onLeft { Logger.e(it) { "Failed to save the text recognition script" } }
    }

    private fun exportSettings() = intent {
        val sideEffect = backupRepository.exportToUri()
            .fold(
                ifLeft = { SideEffect.ExportFailed },
                ifRight = { uri -> SideEffect.ShareExport(uri) },
            )
        postSideEffect(sideEffect)
    }

    private fun showImportConfirmation() = intent {
        reduce { state.copy(dialog = TsDialog.ImportConfirmation) }
    }

    private fun confirmImport() = intent {
        reduce { state.copy(dialog = TsDialog.None) }
        postSideEffect(SideEffect.LaunchFilePicker)
    }

    private fun dismissDialog() = intent {
        reduce { state.copy(dialog = TsDialog.None) }
    }

    /**
     * Reading happens here rather than in the picker callback, which is delivered on the
     * main thread. A document chosen through the Storage Access Framework can be backed by
     * a remote provider, so opening it blocks until the file has been fetched.
     */
    private fun importFrom(uri: Uri) = intent {
        val sideEffect = backupRepository.importFromUri(uri)
            .fold(
                ifLeft = { SideEffect.ImportResult(success = false) },
                ifRight = { SideEffect.ImportResult(success = true) },
            )
        postSideEffect(sideEffect)
    }
}
