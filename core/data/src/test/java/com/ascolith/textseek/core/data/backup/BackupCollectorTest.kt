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
import com.ascolith.textseek.core.testing.MainDispatcherRule
import com.ascolith.textseek.domain.model.AppSetting
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BackupCollectorTest {

    private lateinit var collector: BackupCollector
    private lateinit var groupDao: FakeSearchGroupDao
    private lateinit var settingDao: FakeSearchSettingDao
    private lateinit var appSettings: DefaultAppSettingsRepository

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setup() {
        groupDao = FakeSearchGroupDao()
        settingDao = FakeSearchSettingDao()
        appSettings = DefaultAppSettingsRepository(FakeKeyValueStore())
        collector = BackupCollector(groupDao, settingDao, appSettings, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun `collects the stored groups and settings`() = runTest {
        groupDao.insertAll(listOf(GROUP))
        settingDao.insertAll(listOf(SETTING))

        val backup = collector.collectBackup()

        assertEquals(listOf(GROUP.toBackup()), backup.searchGroups)
        assertEquals(listOf(SETTING.toBackup()), backup.searchSettings)
    }

    @Test
    fun `collects every row from both daos`() = runTest {
        groupDao.insertAll(listOf(GROUP, GROUP.copy(uid = 2L, name = "second")))
        settingDao.insertAll(
            listOf(SETTING, SETTING.copy(uid = 20L, text = "second"), SETTING.copy(uid = 30L, text = "third")),
        )

        val backup = collector.collectBackup()

        assertEquals(2, backup.searchGroups.size)
        assertEquals(3, backup.searchSettings.size)
    }

    @Test
    fun `collects empty collections from an empty database`() = runTest {
        val backup = collector.collectBackup()

        assertEquals(emptyList<BackupSearchGroup>(), backup.searchGroups)
        assertEquals(emptyList<BackupSearchSetting>(), backup.searchSettings)
    }

    @Test
    fun `collects settings even when no groups exist`() = runTest {
        settingDao.insertAll(listOf(SETTING.copy(searchGroupId = null)))

        val backup = collector.collectBackup()

        assertEquals(emptyList<BackupSearchGroup>(), backup.searchGroups)
        assertEquals(1, backup.searchSettings.size)
    }

    @Test
    fun `collects every app setting`() = runTest {
        appSettings.set(AppSetting.GlobalDefaultIndicatorColor, 0xFF1B4D9B.toInt())

        val backup = collector.collectBackup()

        assertEquals(AppSetting.all.map { it.key }.toSet(), backup.appSettings.keys)
        assertEquals("#FF1B4D9B", backup.appSettings["global_default_indicator_color"])
    }

    @Test
    fun `stamps the current backup version`() = runTest {
        assertEquals(BackupData.CURRENT_BACKUP_VERSION, collector.collectBackup().version)
    }

    private companion object {
        val GROUP = DbSearchGroup(uid = 1L, name = "group", enabled = true, colorArgb = 0x11223344)
        val SETTING = DbSearchSetting(
            uid = 10L,
            text = "needle",
            caseSensitive = true,
            color = 0x556677,
            enabled = false,
            searchGroupId = 1L,
        )
    }
}
