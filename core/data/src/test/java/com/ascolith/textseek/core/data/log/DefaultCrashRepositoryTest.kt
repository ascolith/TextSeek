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

    private val crashFiles by lazy { CrashFiles(directory = tempFolder.root) }

    @Test
    fun `no crash is pending when the marker is absent`() = runTest {
        val repository = DefaultCrashRepository(crashFiles, StandardTestDispatcher(testScheduler))

        assertFalse(repository.hasPendingCrash())
    }

    @Test
    fun `a crash is pending while the marker exists`() = runTest {
        crashFiles.pendingMarker.createNewFile()
        val repository = DefaultCrashRepository(crashFiles, StandardTestDispatcher(testScheduler))

        assertTrue(repository.hasPendingCrash())
    }

    @Test
    fun `clearing removes the marker but keeps the crash log`() = runTest {
        crashFiles.pendingMarker.createNewFile()
        crashFiles.crashFile.writeText("trace\n")
        val repository = DefaultCrashRepository(crashFiles, StandardTestDispatcher(testScheduler))

        repository.clearPendingCrash()

        assertFalse(repository.hasPendingCrash())
        assertTrue(crashFiles.crashFile.exists())
    }
}
