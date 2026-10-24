package com.ascolith.textseek.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.ascolith.textseek.core.data.onboarding.IOnboardingRepository
import com.ascolith.textseek.core.data.settings.IAppSettingsRepository
import com.ascolith.textseek.domain.model.AppSetting
import com.ascolith.textseek.domain.model.TextRecognitionScript
import com.ascolith.textseek.feature.onboarding.ui.model.ScreenIntent
import com.ascolith.textseek.feature.onboarding.ui.model.SideEffect
import com.ascolith.textseek.feature.onboarding.ui.model.UiState
import com.ascolith.textseek.resources.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: IOnboardingRepository,
    private val appSettingsRepository: IAppSettingsRepository,
) : ContainerHost<UiState, SideEffect>, ViewModel() {

    override val container = container<UiState, SideEffect>(UiState()) {
        observeTextRecognitionScript()
    }

    fun handleIntent(intent: ScreenIntent) {
        when (intent) {
            ScreenIntent.Back -> goBack()
            ScreenIntent.Forward -> goForward()
            is ScreenIntent.SetPrivacyAgreed -> intent { reduce { state.copy(privacyAgreed = intent.agreed) } }
            is ScreenIntent.SelectScript -> saveTextRecognitionScript(intent.script)
        }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun observeTextRecognitionScript() = subIntent {
        repeatOnSubscription {
            appSettingsRepository.observe(AppSetting.TextRecognitionScript)
                .collect { script -> reduce { state.copy(script = script) } }
        }
    }

    private fun goBack() = intent {
        reduce { state.copy(currentPage = (state.currentPage - 1).coerceAtLeast(0)) }
    }

    private fun goForward() = intent {
        when {
            !state.canGoForward -> Unit
            state.isLastPage -> complete()
            else -> reduce { state.copy(currentPage = state.currentPage + 1) }
        }
    }

    private fun saveTextRecognitionScript(script: TextRecognitionScript) = intent {
        appSettingsRepository.set(AppSetting.TextRecognitionScript, script)
            .onLeft { Logger.e(it) { "Failed to save the text recognition script" } }
    }

    private suspend fun complete() = subIntent {
        onboardingRepository.markCompleted().onLeft {
            Logger.e(it) { "Failed to save that onboarding was completed" }
            postSideEffect(SideEffect.ShowMessage(R.string.error_generic))
        }
    }
}
