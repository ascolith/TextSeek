package com.ascolith.textseek.core.data.backup

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
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BackupApplierTest {

    private lateinit var applier: BackupApplier
    private lateinit var groupDao: FakeSearchGroupDao
    private lateinit var settingDao: FakeSearchSettingDao
    private lateinit var appSettings: DefaultAppSettingsRepository
    private lateinit var keyValueStore: FakeKeyValueStore

    /** Records how the applier used the transaction. */
    private var transactionCount = 0
    private var groupsInsideTransaction: List<DbSearchGroup>? = null
    private var settingsInsideTransaction: List<DbSearchSetting>? = null

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setup() {
        groupDao = FakeSearchGroupDao()
        settingDao = FakeSearchSettingDao()
        keyValueStore = FakeKeyValueStore()
        appSettings = DefaultAppSettingsRepository(keyValueStore)
        applier = BackupApplier(
            searchGroupDao = groupDao,
            searchSettingDao = settingDao,
            transactionRunner = object : ITransactionRunner {
                override suspend fun <T> withTransaction(block: suspend () -> T): T {
                    transactionCount++
                    val result = block()
                    groupsInsideTransaction = groupDao.getSearchGroups().first()
                    settingsInsideTransaction = settingDao.getSearchSettings().first()
                    return result
                }
            },
            appSettingsRepository = appSettings,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    @Test
    fun `writes to both daos`() = runTest {
        applier.applyBackup(RESTORED)

        assertEquals(listOf("restored"), groupDao.getSearchGroups().first().map { it.name })
        assertEquals(listOf("restored"), settingDao.getSearchSettings().first().map { it.text })
    }

    @Test
    fun `restores the app settings`() = runTest {
        applier.applyBackup(RESTORED.copy(appSettings = mapOf("global_default_indicator_color" to "#FF1B4D9B")))

        assertEquals(0xFF1B4D9B.toInt(), appSettings.observe(AppSetting.GlobalDefaultIndicatorColor).first())
    }

    @Test
    fun `a failure writing the app settings is reported`() = runTest {
        val failure = IOException("disk full")
        keyValueStore.writeFailure = failure

        val result = applier.applyBackup(
            RESTORED.copy(appSettings = mapOf("global_default_indicator_color" to "#FF1B4D9B")),
        )

        assertEquals(failure, result.leftOrNull())
    }

    @Test
    fun `replaces existing data rather than merging`() = runTest {
        groupDao.insertAll(listOf(DbSearchGroup(uid = 99L, name = "stale", colorArgb = 1)))
        settingDao.insertAll(listOf(DbSearchSetting(uid = 99L, text = "stale")))

        applier.applyBackup(RESTORED)

        assertEquals(listOf(1L), groupDao.getSearchGroups().first().map { it.uid })
        assertEquals(listOf(10L), settingDao.getSearchSettings().first().map { it.uid })
    }

    @Test
    fun `an empty backup clears both daos`() = runTest {
        groupDao.insertAll(listOf(DbSearchGroup(uid = 1L, name = "group", colorArgb = 1)))
        settingDao.insertAll(listOf(DbSearchSetting(uid = 10L, text = "needle")))

        applier.applyBackup(BackupData(searchGroups = emptyList(), searchSettings = emptyList()))

        assertEquals(emptyList<DbSearchGroup>(), groupDao.getSearchGroups().first())
        assertEquals(emptyList<DbSearchSetting>(), settingDao.getSearchSettings().first())
    }

    @Test
    fun `runs entirely inside a single transaction`() = runTest {
        applier.applyBackup(RESTORED)

        assertEquals(1, transactionCount)
        // Both tables were already written before the transaction block returned.
        assertEquals(listOf("restored"), groupsInsideTransaction?.map { it.name })
        assertEquals(listOf("restored"), settingsInsideTransaction?.map { it.text })
    }

    @Test
    fun `a failing transaction leaves the stored data untouched`() = runTest {
        groupDao.insertAll(listOf(DbSearchGroup(uid = 1L, name = "original", colorArgb = 1)))
        val failing = BackupApplier(
            searchGroupDao = groupDao,
            searchSettingDao = settingDao,
            transactionRunner = object : ITransactionRunner {
                override suspend fun <T> withTransaction(block: suspend () -> T): T =
                    error("transaction could not be opened")
            },
            appSettingsRepository = appSettings,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        assertThrows(IllegalStateException::class.java) {
            runTest { failing.applyBackup(RESTORED) }
        }

        assertEquals(listOf("original"), groupDao.getSearchGroups().first().map { it.name })
    }

    private companion object {
        val RESTORED = BackupData(
            searchGroups = listOf(BackupSearchGroup(1L, "restored", false, 0x11223344)),
            searchSettings = listOf(BackupSearchSetting(10L, "restored", true, 0x556677, false, 1L)),
        )
    }
}
