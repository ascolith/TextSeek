package com.ascolith.textseek.core.data.onboarding

import com.ascolith.textseek.core.data.FakeKeyValueStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultOnboardingRepositoryTest {

    private val repository = DefaultOnboardingRepository(FakeKeyValueStore())

    @Test
    fun `onboarding is not completed until it is marked`() = runTest {
        assertFalse(repository.observeCompleted().first())
    }

    @Test
    fun `marking onboarding completed is persisted`() = runTest {
        repository.markCompleted()

        assertTrue(repository.observeCompleted().first())
    }

    @Test
    fun `resetting makes onboarding incomplete again`() = runTest {
        repository.markCompleted()

        repository.reset()

        assertFalse(repository.observeCompleted().first())
    }
}
