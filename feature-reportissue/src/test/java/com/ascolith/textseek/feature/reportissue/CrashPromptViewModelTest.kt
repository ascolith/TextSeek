package com.ascolith.textseek.feature.reportissue

import com.ascolith.textseek.core.data.log.ICrashRepository
import com.ascolith.textseek.feature.reportissue.ui.crashprompt.CrashPromptViewModel
import com.ascolith.textseek.feature.reportissue.ui.crashprompt.model.ScreenIntent
import com.ascolith.textseek.feature.reportissue.ui.crashprompt.model.SideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class CrashPromptViewModelTest {

    private val crashRepository = FakeCrashRepository()

    private fun buildViewModel() = CrashPromptViewModel(crashRepository = crashRepository)

    @Test
    fun `no dialog is shown when the app did not crash`() = runTest {
        buildViewModel().test(this) {
            runOnCreate()

            expectNoItems()

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the dialog is shown after a crash`() = runTest {
        crashRepository.pending = true

        buildViewModel().test(this) {
            runOnCreate()

            assertTrue(awaitState().showDialog)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `reporting answers the prompt and opens the issue reporter`() = runTest {
        crashRepository.pending = true

        buildViewModel().test(this) {
            runOnCreate()
            awaitState()

            containerHost.handleIntent(ScreenIntent.Report)

            assertFalse(awaitState().showDialog)
            assertEquals(SideEffect.NavigateToReportIssue, awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }

        assertFalse(crashRepository.pending)
    }

    @Test
    fun `dismissing answers the prompt without opening the issue reporter`() = runTest {
        crashRepository.pending = true

        buildViewModel().test(this) {
            runOnCreate()
            awaitState()

            containerHost.handleIntent(ScreenIntent.Dismiss)

            assertFalse(awaitState().showDialog)
            expectNoItems()

            cancelAndIgnoreRemainingItems()
        }

        assertFalse(crashRepository.pending)
    }

    private class FakeCrashRepository : ICrashRepository {
        var pending = false

        override suspend fun hasPendingCrash(): Boolean = pending

        override suspend fun clearPendingCrash() {
            pending = false
        }
    }
}
