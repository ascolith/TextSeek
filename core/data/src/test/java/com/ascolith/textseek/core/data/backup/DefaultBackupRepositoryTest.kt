package com.ascolith.textseek.core.data.backup

import android.net.FakeUri
import android.net.Uri
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.ascolith.textseek.core.data.FakeKeyValueStore
import com.ascolith.textseek.core.data.FakeSearchGroupDao
import com.ascolith.textseek.core.data.FakeSearchSettingDao
import com.ascolith.textseek.core.data.backup.model.BackupData
import com.ascolith.textseek.core.data.backup.model.BackupSearchGroup
import com.ascolith.textseek.core.data.backup.model.BackupSearchSetting
import com.ascolith.textseek.core.data.settings.DefaultAppSettingsRepository
import com.ascolith.textseek.core.database.entity.DbSearchGroup
import com.ascolith.textseek.core.database.entity.DbSearchSetting
import com.ascolith.textseek.core.database.transaction.ITransactionRunner
import com.ascolith.textseek.core.testing.MainDispatcherRule
import com.ascolith.textseek.domain.model.AppSetting
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class DefaultBackupRepositoryTest {

    private lateinit var repository: DefaultBackupRepository
    private lateinit var collector: BackupCollector
    private lateinit var groupDao: FakeSearchGroupDao
    private lateinit var settingDao: FakeSearchSettingDao
    private lateinit var appSettings: DefaultAppSettingsRepository

    /** What the repository handed to the applier, and whether a transaction was ever opened. */
    private var appliedBackup: BackupData? = null
    private var transactionCount = 0

    /** What the repository handed to the file writer. */
    private var writtenFileName: String? = null
    private var writtenContent: String? = null
    private var contentToRead: String = ""
    private var writeFailure: Throwable? = null
    private var applyFailure: Throwable? = null

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val serializer = BackupSerializer()

    @Before
    fun setup() {
        groupDao = FakeSearchGroupDao()
        settingDao = FakeSearchSettingDao()
        appSettings = DefaultAppSettingsRepository(FakeKeyValueStore())
        collector = BackupCollector(groupDao, settingDao, appSettings, mainDispatcherRule.testDispatcher)
        val applier = BackupApplier(
            searchGroupDao = groupDao,
            searchSettingDao = settingDao,
            transactionRunner = object : ITransactionRunner {
                override suspend fun <T> withTransaction(block: suspend () -> T): T {
                    transactionCount++
                    return block()
                }
            },
            appSettingsRepository = appSettings,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
        repository = DefaultBackupRepository(
            backupCollector = collector,
            backupApplier = object : IBackupApplier {
                override suspend fun applyBackup(backupData: BackupData): Either<Throwable, Unit> {
                    applyFailure?.let { return it.left() }
                    appliedBackup = backupData
                    return applier.applyBackup(backupData)
                }
            },
            backupSerializer = serializer,
            cacheFileWriter = { fileName, content ->
                writtenFileName = fileName
                writtenContent = content
                writeFailure?.left() ?: EXPORTED_URI.right()
            },
            uriContentReader = { contentToRead.right() },
            timeProvider = { NOW },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun encoded(backupData: BackupData): String =
        serializer.encode(backupData).getOrElse { throw AssertionError("encode returned Left", it) }

    private fun decoded(json: String): BackupData =
        serializer.decode(json).getOrElse { throw AssertionError("decode returned Left", it) }

    @Test
    fun `export reports a file writer failure instead of throwing past its signature`() = runTest {
        writeFailure = java.io.IOException("no space left on device")

        val result = repository.exportToUri()

        assertTrue(result.isLeft())
    }

    @Test
    fun `import reports an applier failure instead of throwing past its signature`() = runTest {
        contentToRead = encoded(RESTORED)
        applyFailure = android.database.SQLException("database is locked")

        val result = repository.importFromUri(EXPORTED_URI)

        assertTrue(result.isLeft())
    }

    @Test
    fun `export writes the encoded backup under a file name dated today`() = runTest {
        groupDao.insertAll(listOf(GROUP))
        settingDao.insertAll(listOf(SETTING))

        repository.exportToUri()

        assertEquals("txt_seek_backup_2026-09-27.json", writtenFileName)
        assertEquals(collector.collectBackup(), decoded(requireNotNull(writtenContent)))
    }

    @Test
    fun `export returns the uri the file writer produced`() = runTest {
        assertSame(EXPORTED_URI, repository.exportToUri().getOrNull())
    }

    @Test
    fun `export of an empty database still writes a backup`() = runTest {
        repository.exportToUri()

        val written = decoded(requireNotNull(writtenContent))
        assertEquals(emptyList<BackupSearchGroup>(), written.searchGroups)
        assertEquals(emptyList<BackupSearchSetting>(), written.searchSettings)
    }

    @Test
    fun `import decodes the json and hands it to the applier`() = runTest {
        contentToRead = encoded(RESTORED)
        val result = repository.importFromUri(EXPORTED_URI)

        assertTrue(result.isRight())
        assertEquals(RESTORED, appliedBackup)
        assertEquals(listOf("restored"), groupDao.getSearchGroups().first().map { it.name })
    }

    @Test
    fun `import of malformed json reports failure before anything is applied`() = runTest {
        groupDao.insertAll(listOf(GROUP))
        contentToRead = "not json at all"

        val result = repository.importFromUri(EXPORTED_URI)

        assertTrue(result.isLeft())
        assertNull(appliedBackup)
        assertEquals(0, transactionCount)
        assertEquals(listOf(GROUP.name), groupDao.getSearchGroups().first().map { it.name })
    }

    @Test
    fun `a collected backup can be imported back unchanged`() = runTest {
        groupDao.insertAll(listOf(GROUP))
        settingDao.insertAll(listOf(SETTING))
        val exported = encoded(collector.collectBackup())

        contentToRead = exported
        repository.importFromUri(EXPORTED_URI)

        assertEquals(exported, encoded(collector.collectBackup()))
    }

    @Test
    fun `import restores the app settings that were exported`() = runTest {
        appSettings.set(AppSetting.GlobalDefaultIndicatorColor, 0xFF1B4D9B.toInt())
        contentToRead = encoded(collector.collectBackup())
        appSettings.set(AppSetting.GlobalDefaultIndicatorColor, 0xFF000000.toInt())

        repository.importFromUri(EXPORTED_URI)

        assertEquals(0xFF1B4D9B.toInt(), appSettings.observe(AppSetting.GlobalDefaultIndicatorColor).first())
    }

    private companion object {
        /** See [FakeUri]: a real Uri cannot be constructed off-device, but a subclass can. */
        val EXPORTED_URI: Uri = FakeUri("content://test.fileprovider/cache/backup.json")

        /** Midday local time, so the local date is 2026-09-27 whatever zone the test runs in. */
        val NOW: Instant = LocalDate(2026, 9, 27).atTime(LocalTime(12, 0))
            .toInstant(TimeZone.currentSystemDefault())

        val GROUP = DbSearchGroup(uid = 1L, name = "group", enabled = true, colorArgb = 0x11223344)
        val SETTING = DbSearchSetting(
            uid = 10L,
            text = "needle",
            caseSensitive = true,
            color = 0x556677,
            enabled = false,
            searchGroupId = 1L,
        )

        val RESTORED = BackupData(
            searchGroups = listOf(BackupSearchGroup(1L, "restored", false, 0x11223344)),
            searchSettings = listOf(BackupSearchSetting(10L, "restored", true, 0x556677, false, 1L)),
        )
    }
}
