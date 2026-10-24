package com.ascolith.textseek.core.data.log

import java.io.IOException
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashRecorderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val location by lazy {
        LogFileLocation(directory = tempFolder.root, name = "test", maxLogFiles = 3)
    }

    private fun createRecorder(location: LogFileLocation = this.location) =
        CrashRecorder(location) { CRASH_TIME }

    @Test
    fun `record writes the stack trace and its causes`() {
        createRecorder().record("main", crash())

        assertEquals(
            """
            |Crashed at 2026-09-27T12:00:00Z on thread main
            |java.lang.IllegalStateException
            |	at com.example.Foo.bar(Foo.kt:12)
            |Caused by: java.io.IOException
            |	at com.example.Baz.qux(Baz.kt:34)
            |
            """.trimMargin(),
            location.crashFile.readText(),
        )
    }

    @Test
    fun `record leaves out exception messages, which can contain search terms`() {
        createRecorder().record("main", crash())

        val written = location.crashFile.readText()
        assertFalse(written.contains(SECRET))
    }

    @Test
    fun `record marks the crash as pending`() {
        createRecorder().record("main", crash())

        assertTrue(location.crashPendingMarker.exists())
    }

    @Test
    fun `a later crash replaces the earlier one`() {
        val recorder = createRecorder()
        recorder.record("main", crash())

        recorder.record("worker", IllegalArgumentException().withFrame("com.example.Later", "run", "Later.kt", 1))

        assertEquals(
            """
            |Crashed at 2026-09-27T12:00:00Z on thread worker
            |java.lang.IllegalArgumentException
            |	at com.example.Later.run(Later.kt:1)
            |
            """.trimMargin(),
            location.crashFile.readText(),
        )
    }

    @Test
    fun `a cause chain that loops back on itself is written once`() {
        val first = IllegalStateException().withFrame("com.example.A", "a", "A.kt", 1)
        val second = IOException(first).withFrame("com.example.B", "b", "B.kt", 2)
        first.initCause(second)

        createRecorder().record("main", first)

        assertEquals(1, location.crashFile.readLines().count { it == "Caused by: java.io.IOException" })
    }

    @Test
    fun `the handler records the crash and passes it on`() {
        val previous = RecordingHandler()
        val crash = crash()

        createRecorder().handlerWrapping(previous).uncaughtException(Thread.currentThread(), crash)

        assertTrue(location.crashPendingMarker.exists())
        assertSame(crash, previous.received)
    }

    @Test
    fun `the handler still passes the crash on when it cannot be recorded`() {
        val previous = RecordingHandler()
        val crash = crash()
        val unwritable = location.copy(directory = tempFolder.root.resolve("missing"))

        createRecorder(unwritable).handlerWrapping(previous).uncaughtException(Thread.currentThread(), crash)

        assertSame(crash, previous.received)
    }

    private class RecordingHandler : Thread.UncaughtExceptionHandler {
        var received: Throwable? = null

        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            received = throwable
        }
    }

    private fun crash(): Throwable {
        val cause = IOException(SECRET).withFrame("com.example.Baz", "qux", "Baz.kt", 34)
        return IllegalStateException(SECRET, cause).withFrame("com.example.Foo", "bar", "Foo.kt", 12)
    }

    private fun <T : Throwable> T.withFrame(className: String, method: String, file: String, line: Int): T =
        apply { stackTrace = arrayOf(StackTraceElement(className, method, file, line)) }

    private companion object {
        val CRASH_TIME: Instant = Instant.parse("2026-09-27T12:00:00Z")
        const val SECRET = "user search term"
    }
}
