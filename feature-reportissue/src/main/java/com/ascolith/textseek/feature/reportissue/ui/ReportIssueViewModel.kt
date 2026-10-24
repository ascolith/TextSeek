package com.ascolith.textseek.feature.reportissue.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import co.touchlab.kermit.Logger
import com.ascolith.textseek.core.data.file.ICacheFileWriter
import com.ascolith.textseek.core.data.log.ILogRepository
import com.ascolith.textseek.domain.feature.stringprovider.IStringProvider
import com.ascolith.textseek.domain.feature.version.IAppVersionProvider
import com.ascolith.textseek.domain.feature.version.IDeviceInfoProvider
import com.ascolith.textseek.feature.reportissue.ui.email.IEmailLauncher
import com.ascolith.textseek.feature.reportissue.ui.model.ScreenIntent
import com.ascolith.textseek.feature.reportissue.ui.model.SideEffect
import com.ascolith.textseek.feature.reportissue.ui.model.UiState
import com.ascolith.textseek.resources.R
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class ReportIssueViewModel @Inject constructor(
    private val logRepository: ILogRepository,
    private val cacheFileWriter: ICacheFileWriter,
    private val appVersionProvider: IAppVersionProvider,
    private val deviceInfoProvider: IDeviceInfoProvider,
    private val emailLauncher: IEmailLauncher,
    private val stringProvider: IStringProvider,
) : ContainerHost<UiState, SideEffect>, ViewModel() {

    override val container = container<UiState, SideEffect>(UiState())

    fun handleIntent(intent: ScreenIntent) {
        when (intent) {
            is ScreenIntent.DescriptionChanged -> updateDescription(intent.text)
            is ScreenIntent.IncludeLogsChanged -> updateIncludeLogs(intent.includeLogs)
            ScreenIntent.Send -> send()
        }
    }

    private fun updateDescription(text: String) = intent {
        reduce { state.copy(description = text.take(UiState.MAX_DESCRIPTION_LENGTH)) }
    }

    private fun updateIncludeLogs(includeLogs: Boolean) = intent {
        reduce { state.copy(includeLogs = includeLogs) }
    }

    private fun send() = intent {
        if (!state.canSend) return@intent
        reduce { state.copy(isSending = true) }

        val attachment = if (state.includeLogs) prepareLogAttachment() else null.right()
        val body = buildBody(state.description)

        reduce { state.copy(isSending = false) }
        attachment.fold(
            ifLeft = { postSideEffect(SideEffect.AttachLogsFailed) },
            ifRight = { uri ->
                val emailLaunched = emailLauncher.launch(
                    subject = stringProvider.getString(R.string.report_issue_email_subject),
                    body = body,
                    attachment = uri
                )
                if (!emailLaunched) postSideEffect(SideEffect.NoEmailApp)
            },
        )
    }

    private suspend fun prepareLogAttachment(): Either<Throwable, Uri> =
        logRepository.readLogs()
            .onLeft { Logger.e { "Failed to read logs for an issue report: ${it::class.simpleName}" } }
            .flatMap { logs -> cacheFileWriter.writeToCache(LOG_ATTACHMENT_FILE_NAME, logs) }

    private fun buildBody(description: String): String = buildString {
        appendLine(description.trim())
        appendLine()
        appendLine("---")
        appendLine("App version: ${appVersionProvider.versionName} (${appVersionProvider.versionCode})")
        appendLine("Android: ${deviceInfoProvider.androidVersion} (API ${deviceInfoProvider.sdkInt})")
        append("Device: ${deviceInfoProvider.deviceModel}")
    }

    private companion object {
        const val LOG_ATTACHMENT_FILE_NAME = "text-seek-logs.txt"
    }
}
