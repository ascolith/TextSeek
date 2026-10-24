package com.ascolith.textseek.core.data.backup

import com.ascolith.textseek.core.data.backup.model.BackupSearchGroup
import com.ascolith.textseek.core.data.backup.model.BackupSearchSetting
import com.ascolith.textseek.core.database.entity.DbSearchGroup
import com.ascolith.textseek.core.database.entity.DbSearchSetting
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMapperExtensionsTest {

    @Test
    fun `group maps to backup field for field`() {
        val backup = DB_GROUP.toBackup()

        assertEquals(1L, backup.uid)
        assertEquals("group", backup.name)
        assertEquals(true, backup.enabled)
        assertEquals(0x11223344, backup.colorArgb)
    }

    @Test
    fun `setting maps to backup field for field`() {
        val backup = DB_SETTING.toBackup()

        assertEquals(10L, backup.uid)
        assertEquals("needle", backup.text)
        assertEquals(true, backup.caseSensitive)
        assertEquals(0x556677, backup.color)
        assertEquals(false, backup.enabled)
        assertEquals(1L, backup.searchGroupId)
    }

    @Test
    fun `group maps back from backup field for field`() {
        val db = BACKUP_GROUP.toDbModel()

        assertEquals(2L, db.uid)
        assertEquals("restored", db.name)
        assertEquals(false, db.enabled)
        assertEquals(0x0F0F0F, db.colorArgb)
    }

    @Test
    fun `setting maps back from backup field for field`() {
        val db = BACKUP_SETTING.toDbModel()

        assertEquals(20L, db.uid)
        assertEquals("restored", db.text)
        assertEquals(false, db.caseSensitive)
        assertEquals(0x0F0F0F, db.color)
        assertEquals(true, db.enabled)
        assertEquals(2L, db.searchGroupId)
    }

    @Test
    fun `setting keeps a null color and a null group id in both directions`() {
        val db = DB_SETTING.copy(color = null, searchGroupId = null)

        val backup = db.toBackup()
        assertEquals(null, backup.color)
        assertEquals(null, backup.searchGroupId)

        val roundTripped = backup.toDbModel()
        assertEquals(null, roundTripped.color)
        assertEquals(null, roundTripped.searchGroupId)
    }

    @Test
    fun `group survives a round trip unchanged`() {
        assertEquals(DB_GROUP, DB_GROUP.toBackup().toDbModel())
    }

    @Test
    fun `setting survives a round trip unchanged`() {
        assertEquals(DB_SETTING, DB_SETTING.toBackup().toDbModel())
    }

    @Test
    fun `a setting with default values survives a round trip unchanged`() {
        val defaults = DbSearchSetting()

        assertEquals(defaults, defaults.toBackup().toDbModel())
    }

    private companion object {
        val DB_GROUP = DbSearchGroup(uid = 1L, name = "group", enabled = true, colorArgb = 0x11223344)
        val DB_SETTING = DbSearchSetting(
            uid = 10L,
            text = "needle",
            caseSensitive = true,
            color = 0x556677,
            enabled = false,
            searchGroupId = 1L,
        )

        val BACKUP_GROUP = BackupSearchGroup(
            uid = 2L,
            name = "restored",
            enabled = false,
            colorArgb = 0x0F0F0F,
        )
        val BACKUP_SETTING = BackupSearchSetting(
            uid = 20L,
            text = "restored",
            caseSensitive = false,
            color = 0x0F0F0F,
            enabled = true,
            searchGroupId = 2L,
        )
    }
}
