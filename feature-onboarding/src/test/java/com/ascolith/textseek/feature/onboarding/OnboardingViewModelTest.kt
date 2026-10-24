package com.ascolith.textseek.feature.onboarding

import arrow.core.Either
import arrow.core.right
import com.ascolith.textseek.core.data.onboarding.IOnboardingRepository
import com.ascolith.textseek.core.data.settings.IAppSettingsRepository
import com.ascolith.textseek.domain.model.AppSetting
import com.ascolith.textseek.domain.model.TextRecognitionScript
import com.ascolith.textseek.feature.onboarding.ui.OnboardingViewModel
import com.ascolith.textseek.feature.onboarding.ui.model.OnboardingPage
import com.ascolith.textseek.feature.onboarding.ui.model.ScreenIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

class OnboardingViewModelTest {

    private val onboardingRepository = FakeOnboardingRepository()
    private val appSettingsRepository = FakeAppSettingsRepository()

    private fun buildViewModel() = OnboardingViewModel(onboardingRepository, appSettingsRepository)

    @Test
    fun `forward is blocked until the privacy policy is agreed to`() = runTest {
        buildViewModel().test(this) {
            runOnCreate()

            containerHost.handleIntent(ScreenIntent.Forward)
            expectNoItems()

            containerHost.handleIntent(ScreenIntent.SetPrivacyAgreed(true))
            assertTrue(awaitState().privacyAgreed)
            containerHost.handleIntent(ScreenIntent.Forward)
            assertEquals(OnboardingPage.CameraPermission, awaitState().page)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `back returns to the previous page`() = runTest {
        buildViewModel().test(this) {
            runOnCreate()
            containerHost.handleIntent(ScreenIntent.SetPrivacyAgreed(true))
            awaitState()
            containerHost.handleIntent(ScreenIntent.Forward)
            awaitState()

            containerHost.handleIntent(ScreenIntent.Back)

            assertEquals(OnboardingPage.PrivacyPolicy, awaitState().page)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `selecting a script saves it`() = runTest {
        buildViewModel().test(this) {
            runOnCreate()

            containerHost.handleIntent(ScreenIntent.SelectScript(TextRecognitionScript.JAPANESE))

            assertEquals(TextRecognitionScript.JAPANESE, awaitState().script)
            assertEquals("JAPANESE", appSettingsRepository.stored[AppSetting.TextRecognitionScript.key])

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `forward on the last page completes onboarding`() = runTest {
        buildViewModel().test(this) {
            runOnCreate()
            containerHost.handleIntent(ScreenIntent.SetPrivacyAgreed(true))
            awaitState()
            repeat(2) {
                containerHost.handleIntent(ScreenIntent.Forward)
                awaitState()
            }

            containerHost.handleIntent(ScreenIntent.Forward)

            onboardingRepository.completion.await()
            cancelAndIgnoreRemainingItems()
        }
    }

    private class FakeOnboardingRepository : IOnboardingRepository {
        private val completed = MutableStateFlow(false)
        val completion = CompletableDeferred<Unit>()

        override fun observeCompleted(): Flow<Boolean> = completed

        override suspend fun markCompleted(): Either<Throwable, Unit> {
            completed.value = true
            completion.complete(Unit)
            return Unit.right()
        }

        override suspend fun reset(): Either<Throwable, Unit> {
            completed.value = false
            return Unit.right()
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
}
