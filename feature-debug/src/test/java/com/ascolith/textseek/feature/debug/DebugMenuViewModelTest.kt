package com.ascolith.textseek.feature.debug

import com.ascolith.textseek.feature.debug.ui.DebugMenuViewModel
import com.ascolith.textseek.feature.debug.ui.SampleDataSeeder
import com.ascolith.textseek.feature.debug.ui.model.ScreenIntent
import com.ascolith.textseek.feature.debug.ui.model.SideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class DebugMenuViewModelTest {

    private val crashRepository = FakeCrashRepository()
    private val onboardingRepository = FakeOnboardingRepository()
    private val searchGroupRepository = FakeSearchGroupRepository()
    private val searchSettingRepository = FakeSearchSettingRepository()

    private fun buildViewModel() = DebugMenuViewModel(
        crashRepository = crashRepository,
        onboardingRepository = onboardingRepository,
        sampleDataSeeder = SampleDataSeeder(searchGroupRepository, searchSettingRepository),
    )

    @Test
    fun `a pending crash is shown on open`() = runTest {
        crashRepository.pending = true

        buildViewModel().test(this) {
            runOnCreate()

            assertTrue(awaitState().hasPendingCrash)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `clearing the pending crash clears it and confirms`() = runTest {
        crashRepository.pending = true

        buildViewModel().test(this) {
            runOnCreate()
            awaitState() // pending crash

            containerHost.handleIntent(ScreenIntent.ClearPendingCrash)

            assertFalse(awaitState().hasPendingCrash)
            assertEquals(SideEffect.ShowMessage(R.string.debug_pending_crash_cleared), awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }

        assertFalse(crashRepository.pending)
    }

    @Test
    fun `seeding sample data adds searches and confirms`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.SeedSampleData)

            assertEquals(SideEffect.ShowMessage(R.string.debug_sample_data_seeded), awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }

        assertTrue(searchSettingRepository.searchSettings.value.isNotEmpty())
    }

    @Test
    fun `forcing a crash asks the screen to crash`() = runTest {
        buildViewModel().test(this) {
            containerHost.handleIntent(ScreenIntent.ForceCrash)

            assertEquals(SideEffect.Crash, awaitSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `resetting onboarding makes it show again`() = runTest {
        buildViewModel().test(this) {
            runOnCreate()

            containerHost.handleIntent(ScreenIntent.ResetOnboarding)

            onboardingRepository.completed.first { !it }
            cancelAndIgnoreRemainingItems()
        }
    }
}
