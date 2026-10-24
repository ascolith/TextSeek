package com.ascolith.textseek.core.data.log

import co.touchlab.kermit.Severity
import co.touchlab.kermit.io.RollingFileLogWriter
import co.touchlab.kermit.io.RollingFileLogWriterConfig
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.files.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultLogRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val location by lazy {
        LogFileLocation(directory = tempFolder.root, name = "test", maxLogFiles = 3)
    }

    private fun createRepository(dispatcher: TestDispatcher) =
        DefaultLogRepository(location, dispatcher)

    private fun writeLogFile(fileName: String, content: String) {
        tempFolder.root.resolve(fileName).writeText(content)
    }

    @Test
    fun `readLogs returns an empty string when no log files exist`() = runTest {
        val repository = createRepository(StandardTestDispatcher(testScheduler))

        assertEquals("", repository.readLogs().getOrNull())
    }

    @Test
    fun `readLogs joins rolled files oldest first`() = runTest {
        writeLogFile("test.log", "newest\n")
        writeLogFile("test-1.log", "middle\n")
        writeLogFile("test-2.log", "oldest\n")
        val repository = createRepository(StandardTestDispatcher(testScheduler))

        assertEquals("oldest\nmiddle\nnewest\n", repository.readLogs().getOrNull())
    }

    @Test
    fun `readLogs skips missing indexes`() = runTest {
        writeLogFile("test.log", "newest\n")
        writeLogFile("test-2.log", "oldest\n")
        val repository = createRepository(StandardTestDispatcher(testScheduler))

        assertEquals("oldest\nnewest\n", repository.readLogs().getOrNull())
    }

    @Test
    fun `readLogs ignores files beyond maxLogFiles`() = runTest {
        writeLogFile("test.log", "newest\n")
        writeLogFile("test-3.log", "stale\n")
        val repository = createRepository(StandardTestDispatcher(testScheduler))

        assertEquals("newest\n", repository.readLogs().getOrNull())
    }

    @Test
    fun `readLogs appends the last crash after the rolled files`() = runTest {
        writeLogFile("test.log", "newest\n")
        writeLogFile("test-1.log", "oldest\n")
        location.crashFile.writeText("trace\n")
        val repository = createRepository(StandardTestDispatcher(testScheduler))

        assertEquals("oldest\nnewest\n--- Last crash ---\ntrace\n", repository.readLogs().getOrNull())
    }

    @Test
    fun `readLogs returns Left when a log file cannot be read`() = runTest {
        tempFolder.root.resolve("test.log").mkdir()
        val repository = createRepository(StandardTestDispatcher(testScheduler))

        assertTrue(repository.readLogs().isLeft())
    }

    @Test
    fun `readLogs sees what Kermit's RollingFileLogWriter wrote`() = runTest {
        val writer = RollingFileLogWriter(
            RollingFileLogWriterConfig(
                logFileName = location.name,
                logFilePath = Path(location.directory.path),
                maxLogFiles = location.maxLogFiles,
            ),
        )
        val repository = createRepository(StandardTestDispatcher(testScheduler))

        writer.log(Severity.Warn, "round trip", "Tag", null)

        // The writer flushes on its own thread, so poll in real time until the line lands.
        withContext(Dispatchers.Default) {
            withTimeout(5.seconds) {
                while (location.existingFilesOldestFirst().none { "round trip" in it.readText() }) {
                    delay(10)
                }
            }
        }
        assertTrue(repository.readLogs().getOrNull().orEmpty().contains("round trip"))
    }
}
