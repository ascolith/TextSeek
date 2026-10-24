package com.ascolith.textseek.core.data.file

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import arrow.core.getOrElse
import com.ascolith.textseek.core.data.backup.DefaultBackupRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [CacheFileWriter]. This is the only part of the backup flow that needs a
 * device: everything else is covered off-device by `DefaultBackupRepositoryTest`.
 */
@RunWith(AndroidJUnit4::class)
class CacheFileWriterInstrumentedTest {

    private lateinit var context: Context
    private lateinit var cacheFileWriter: CacheFileWriter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        cacheFileWriter = CacheFileWriter(context, Dispatchers.IO)
        deleteTestFiles()
    }

    @After
    fun tearDown() {
        deleteTestFiles()
    }

    private fun deleteTestFiles() {
        listOf(FILE_NAME, OTHER_FILE_NAME, BACKUP_FILE_NAME)
            .forEach { File(context.cacheDir, it).delete() }
    }

    private fun cacheFile() = File(context.cacheDir, FILE_NAME)

    private suspend fun write(fileName: String, content: String): Uri =
        cacheFileWriter.writeToCache(fileName, content)
            .getOrElse { throw AssertionError("writeToCache returned Left", it) }

    private fun readBack(uri: Uri): String =
        requireNotNull(context.contentResolver.openInputStream(uri)).use {
            it.readBytes().decodeToString()
        }

    @Test
    fun writeToCache_returnsAReadableContentUri() = runBlocking {
        val uri = write(FILE_NAME, """{"hello":"world"}""")

        assertEquals("content", uri.scheme)
        assertEquals("""{"hello":"world"}""", readBack(uri))
    }

    @Test
    fun writeToCache_usesTheProviderAuthorityDeclaredForThisPackage() = runBlocking {
        val uri = write(FILE_NAME, "{}")

        assertEquals("${context.packageName}.fileprovider", uri.authority)
    }

    @Test
    fun writeToCache_writesUnderTheGivenNameInTheCacheDirectory() = runBlocking {
        write(FILE_NAME, "{}")

        assertTrue(cacheFile().exists())
    }

    @Test
    fun writeToCache_overwritesAnyPreviousFileOfTheSameName() = runBlocking {
        write(FILE_NAME, """{"first":true}""")
        val uri = write(FILE_NAME, """{"second":true}""")

        assertEquals("""{"second":true}""", readBack(uri))
    }

    @Test
    fun writeToCache_keepsSeparateNamesInSeparateFiles() = runBlocking {
        val first = write(FILE_NAME, """{"first":true}""")
        val second = write(OTHER_FILE_NAME, """{"second":true}""")

        assertEquals("""{"first":true}""", readBack(first))
        assertEquals("""{"second":true}""", readBack(second))
    }

    @Test
    fun writeToCache_writesTheNameTheBackupRepositoryAsksFor() = runBlocking {
        write(BACKUP_FILE_NAME, "{}")

        assertTrue(File(context.cacheDir, BACKUP_FILE_NAME).exists())
    }

    private companion object {
        const val FILE_NAME = "cache_file_writer_test.json"
        const val OTHER_FILE_NAME = "cache_file_writer_test_other.json"
        val BACKUP_FILE_NAME = DefaultBackupRepository.backupFileName(LocalDate(2026, 9, 27))
    }
}
