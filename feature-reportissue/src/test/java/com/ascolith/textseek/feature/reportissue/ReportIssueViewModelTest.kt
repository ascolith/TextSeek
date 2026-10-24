package com.ascolith.textseek.feature.reportissue

import android.net.FakeUri
import android.net.Uri
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.ascolith.textseek.core.data.file.ICacheFileWriter
import com.ascolith.textseek.core.data.log.ILogRepository
import com.ascolith.textseek.domain.feature.stringprovider.IStringProvider
import com.ascolith.textseek.domain.feature.version.IAppVersionProvider
import com.ascolith.textseek.domain.feature.version.IDeviceInfoProvider
import com.ascolith.textseek.feature.reportissue.ui.ReportIssueViewModel
import com.ascolith.textseek.feature.reportissue.ui.email.IEmailLauncher
import com.ascolith.textseek.feature.reportissue.ui.model.ScreenIntent
import com.ascolith.textseek.feature.reportissue.ui.model.SideEffect
import com.ascolith.textseek.feature.reportissue.ui.model.UiState
import com.ascolith.textseek.resources.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ReportIssueViewModelTest {

    private val logRepository = FakeLogRepository()
    private val cacheFileWriter = FakeCacheFileWriter()
    private val emailLauncher = FakeIssueEmailLauncher()

    private fun buildViewModel() = ReportIssueViewModel(
        logRepository = logRepository,
        cacheFileWriter = cacheFileWriter,
        appVersionProvider = FakeAppVersionProvider(),
        deviceInfoProvider = FakeDeviceInfoProvider(),
        emailLauncher = emailLauncher,
        stringProvider = FakeStringProvider(),
    )

    @Test
    fun `the description is capped at the maximum length`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.DescriptionChanged("a".repeat(UiState.MAX_DESCRIPTION_LENGTH + 1)))

            assertEquals(UiState.MAX_DESCRIPTION_LENGTH, awaitState().description.length)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `logs are included by default and can be turned off`() = runTest {
        val viewModel = buildViewModel()
        assertTrue(viewModel.container.stateFlow.value.includeLogs)

        viewModel.test(this) {
            containerHost.handleIntent(ScreenIntent.IncludeLogsChanged(false))

            assertFalse(awaitState().includeLogs)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `sending with logs attaches the logs written to the cache`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.DescriptionChanged(DESCRIPTION))
            awaitState()

            containerHost.handleIntent(ScreenIntent.Send)

            assertTrue(awaitState().isSending)
            assertFalse(awaitState().isSending)

            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf("text-seek-logs.txt" to LOGS), cacheFileWriter.written)
        assertEquals(listOf(EXPECTED_BODY to LOGS_URI), emailLauncher.launched)
        assertEquals(listOf(SUBJECT), emailLauncher.subjects)
    }

    @Test
    fun `sending without logs neither reads nor attaches them`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.DescriptionChanged(DESCRIPTION))
            awaitState()
            containerHost.handleIntent(ScreenIntent.IncludeLogsChanged(false))
            awaitState()

            containerHost.handleIntent(ScreenIntent.Send)

            assertTrue(awaitState().isSending)
            assertFalse(awaitState().isSending)

            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf(EXPECTED_BODY to null), emailLauncher.launched)
        assertEquals(0, logRepository.reads)
        assertEquals(emptyList<Pair<String, String>>(), cacheFileWriter.written)
    }

    @Test
    fun `unreadable logs are reported instead of sending without them`() = runTest {
        logRepository.failure = IOException("disk error")

        assertAttachLogsFailed()
    }

    @Test
    fun `logs that cannot be written for sharing are reported instead of sending without them`() = runTest {
        cacheFileWriter.failure = IOException("no space left")

        assertAttachLogsFailed()
    }

    @Test
    fun `a missing email app is reported`() = runTest {
        emailLauncher.emailAppInstalled = false

        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.DescriptionChanged(DESCRIPTION))
            awaitState()

            containerHost.handleIntent(ScreenIntent.Send)

            assertTrue(awaitState().isSending)
            assertFalse(awaitState().isSending)
            assertEquals(SideEffect.NoEmailApp, awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a blank description is not sent`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.DescriptionChanged("   "))
            awaitState()

            containerHost.handleIntent(ScreenIntent.Send)

            expectNoItems()

            cancelAndIgnoreRemainingItems()
        }
    }

    private suspend fun kotlinx.coroutines.test.TestScope.assertAttachLogsFailed() {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.DescriptionChanged(DESCRIPTION))
            awaitState()

            containerHost.handleIntent(ScreenIntent.Send)

            assertTrue(awaitState().isSending)
            assertFalse(awaitState().isSending)
            assertEquals(SideEffect.AttachLogsFailed, awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }

        assertEquals(emptyList<Pair<String, Uri?>>(), emailLauncher.launched)
    }

    private class FakeLogRepository : ILogRepository {
        var failure: Throwable? = null
        var reads = 0

        override suspend fun readLogs(): Either<Throwable, String> {
            reads++
            return failure?.left() ?: LOGS.right()
        }
    }

    private class FakeCacheFileWriter : ICacheFileWriter {
        var failure: Throwable? = null
        val written = mutableListOf<Pair<String, String>>()

        override suspend fun writeToCache(fileName: String, content: String): Either<Throwable, Uri> {
            failure?.let { return it.left() }
            written += fileName to content
            return LOGS_URI.right()
        }
    }

    private class FakeIssueEmailLauncher : IEmailLauncher {
        var emailAppInstalled = true
        val launched = mutableListOf<Pair<String, Uri?>>()
        val subjects = mutableListOf<String>()

        override fun launch(subject: String, body: String, attachment: Uri?): Boolean {
            if (!emailAppInstalled) return false
            launched += body to attachment
            subjects += subject
            return true
        }
    }

    private class FakeStringProvider : IStringProvider {
        private val strings = mapOf(R.string.report_issue_email_subject to SUBJECT)

        override fun getString(resId: Int): String =
            strings[resId] ?: error("No string stubbed for resource $resId")

        override fun getString(resId: Int, vararg args: Any): String = getString(resId)
    }

    private class FakeAppVersionProvider : IAppVersionProvider {
        override val versionName: String = "1.2.3"
        override val versionCode: Long = 42L
    }

    private class FakeDeviceInfoProvider : IDeviceInfoProvider {
        override val androidVersion: String = "15"
        override val sdkInt: Int = 35
        override val deviceModel: String = "Google Pixel 8"
    }

    private companion object {
        val LOGS_URI: Uri = FakeUri("content://test.fileprovider/cache/text-seek-logs.txt")
        const val LOGS = "I/App: started\n"
        const val SUBJECT = "TextSeek Issue Report"
        const val DESCRIPTION = "The camera freezes when I rotate the phone."
        val EXPECTED_BODY = """
            |The camera freezes when I rotate the phone.
            |
            |---
            |App version: 1.2.3 (42)
            |Android: 15 (API 35)
            |Device: Google Pixel 8
        """.trimMargin()
    }
}
