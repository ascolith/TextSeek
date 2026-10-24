package com.ascolith.textseek.feature.reportissue.ui.crashprompt

import androidx.lifecycle.ViewModel
import com.ascolith.textseek.core.data.log.ICrashRepository
import com.ascolith.textseek.feature.reportissue.ui.crashprompt.model.ScreenIntent
import com.ascolith.textseek.feature.reportissue.ui.crashprompt.model.SideEffect
import com.ascolith.textseek.feature.reportissue.ui.crashprompt.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class CrashPromptViewModel @Inject constructor(
    private val crashRepository: ICrashRepository,
) : ContainerHost<UiState, SideEffect>, ViewModel() {

    override val container = container<UiState, SideEffect>(UiState()) {
        if (crashRepository.hasPendingCrash()) {
            reduce { state.copy(showDialog = true) }
        }
    }

    fun handleIntent(intent: ScreenIntent) {
        when (intent) {
            ScreenIntent.Report -> answer(reportIssue = true)
            ScreenIntent.Dismiss -> answer(reportIssue = false)
        }
    }

    // The crash stays pending until answered, so the prompt returns if the app dies first.
    private fun answer(reportIssue: Boolean) = intent {
        crashRepository.clearPendingCrash()
        reduce { state.copy(showDialog = false) }
        if (reportIssue) postSideEffect(SideEffect.NavigateToReportIssue)
    }
}
