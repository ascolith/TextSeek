package com.ascolith.textseek.core.data.file

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [UriContentReader]. It needs a device because only a real ContentResolver
 * produces the exceptions under test.
 */
@RunWith(AndroidJUnit4::class)
class UriContentReaderInstrumentedTest {

    private lateinit var context: Context
    private lateinit var reader: UriContentReader
    private val log = StringBuilder()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        reader = UriContentReader(context, Dispatchers.IO)
        Logger.setLogWriters(
            object : LogWriter() {
                // Mirrors RollingFileLogWriter, which writes the message and the full stack trace.
                override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                    log.appendLine(message)
                    throwable?.let { log.appendLine(it.stackTraceToString()) }
                }
            },
        )
    }

    @After
    fun tearDown() {
        Logger.setLogWriters(platformLogWriter())
    }

    @Test
    fun readFailure_keepsTheFileNameOutOfTheLog() = runBlocking {
        // Canary: the log is what a bug report sends, and a picked file's name can identify the user.
        val missingFile = File(context.cacheDir, "Jane_Canary_555-0100.json").apply { delete() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", missingFile)

        val result = reader.readText(uri)

        assertTrue(result.isLeft())
        assertTrue(log.toString(), log.contains("Failed to read"))
        assertFalse(log.toString(), log.contains("Canary"))
    }
}
