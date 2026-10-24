package com.ascolith.textseek.feature.reportissue.ui.crashprompt

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ascolith.textseek.core.composable.TsAlertDialog
import com.ascolith.textseek.feature.reportissue.ui.crashprompt.model.ScreenIntent
import com.ascolith.textseek.feature.reportissue.ui.crashprompt.model.SideEffect
import com.ascolith.textseek.resources.R
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

/** Offers to report the crash that ended the previous session, if there was one. */
@Composable
fun CrashPrompt(
    onReportIssue: () -> Unit,
    viewModel: CrashPromptViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            SideEffect.NavigateToReportIssue -> onReportIssue()
        }
    }

    if (state.showDialog) {
        TsAlertDialog(
            titleResId = R.string.crash_dialog_title,
            textResId = R.string.crash_dialog_message,
            confirmTextResId = R.string.report,
            confirmTextColor = MaterialTheme.colorScheme.primary,
            dismissTextResId = R.string.not_now,
            onConfirm = { viewModel.handleIntent(ScreenIntent.Report) },
            onDismiss = { viewModel.handleIntent(ScreenIntent.Dismiss) },
        )
    }
}
