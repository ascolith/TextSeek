package com.ascolith.textseek.core.data.log

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultCrashRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val location by lazy {
        LogFileLocation(directory = tempFolder.root, name = "test", maxLogFiles = 3)
    }

    @Test
    fun `no crash is pending when the marker is absent`() = runTest {
        val repository = DefaultCrashRepository(location, StandardTestDispatcher(testScheduler))

        assertFalse(repository.hasPendingCrash())
    }

    @Test
    fun `a crash is pending while the marker exists`() = runTest {
        location.crashPendingMarker.createNewFile()
        val repository = DefaultCrashRepository(location, StandardTestDispatcher(testScheduler))

        assertTrue(repository.hasPendingCrash())
    }

    @Test
    fun `clearing removes the marker but keeps the crash log`() = runTest {
        location.crashPendingMarker.createNewFile()
        location.crashFile.writeText("trace\n")
        val repository = DefaultCrashRepository(location, StandardTestDispatcher(testScheduler))

        repository.clearPendingCrash()

        assertFalse(repository.hasPendingCrash())
        assertTrue(location.crashFile.exists())
    }
}
