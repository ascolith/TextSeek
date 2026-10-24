package com.ascolith.textseek.feature.debug.ui

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.ascolith.textseek.core.data.log.ICrashRepository
import com.ascolith.textseek.core.data.onboarding.IOnboardingRepository
import com.ascolith.textseek.feature.debug.R
import com.ascolith.textseek.feature.debug.ui.model.ScreenIntent
import com.ascolith.textseek.feature.debug.ui.model.SideEffect
import com.ascolith.textseek.feature.debug.ui.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class DebugMenuViewModel @Inject constructor(
    private val crashRepository: ICrashRepository,
    private val onboardingRepository: IOnboardingRepository,
    private val sampleDataSeeder: SampleDataSeeder,
) : ContainerHost<UiState, SideEffect>, ViewModel() {

    override val container = container<UiState, SideEffect>(UiState()) {
        loadPendingCrash()
    }

    fun handleIntent(intent: ScreenIntent) {
        when (intent) {
            ScreenIntent.ForceCrash -> intent { postSideEffect(SideEffect.Crash) }
            ScreenIntent.ClearPendingCrash -> clearPendingCrash()
            ScreenIntent.SeedSampleData -> seedSampleData()
            ScreenIntent.ResetOnboarding -> resetOnboarding()
        }
    }

    private fun clearPendingCrash() = intent {
        crashRepository.clearPendingCrash()
        loadPendingCrash()
        postSideEffect(SideEffect.ShowMessage(R.string.debug_pending_crash_cleared))
    }

    private suspend fun loadPendingCrash() = subIntent {
        val hasPendingCrash = crashRepository.hasPendingCrash()
        reduce { state.copy(hasPendingCrash = hasPendingCrash) }
    }

    // The app swaps to onboarding as soon as it's reset
    private fun resetOnboarding() = intent {
        onboardingRepository.reset()
            .onLeft { Logger.e(it) { "Failed to reset onboarding" } }
    }

    private fun seedSampleData() = intent {
        sampleDataSeeder.seed()
        postSideEffect(SideEffect.ShowMessage(R.string.debug_sample_data_seeded))
    }
}
