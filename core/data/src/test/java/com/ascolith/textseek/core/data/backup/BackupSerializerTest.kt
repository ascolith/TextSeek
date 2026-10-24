package com.ascolith.textseek.core.data.backup

import arrow.core.getOrElse
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import com.ascolith.textseek.core.data.backup.model.BackupData
import com.ascolith.textseek.core.data.backup.model.BackupSearchGroup
import com.ascolith.textseek.core.data.backup.model.BackupSearchSetting
import kotlinx.serialization.SerializationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the exported backup format. These deliberately assert on raw JSON text as well as on
 * decoded values: a backup written by this version has to stay readable by later ones, and a
 * decode-only assertion would pass even for a field that never reached the file.
 */
class BackupSerializerTest {

    private val serializer = BackupSerializer()

    @After
    fun restoreLogWriters() {
        Logger.setLogWriters(platformLogWriter())
    }

    private fun encode(backupData: BackupData): String =
        serializer.encode(backupData).getOrElse { throw AssertionError("encode returned Left", it) }

    private fun decode(json: String): BackupData =
        serializer.decode(json).getOrElse { throw AssertionError("decode returned Left", it) }

    @Test
    fun `encode writes the version stamp into the json`() {
        // version defaults to CURRENT_BACKUP_VERSION, so it only reaches the file if the encoder is
        // configured to emit defaults.
        assertTrue(encode(BACKUP).contains("\"version\""))
    }

    @Test
    fun `encode writes every group and setting field`() {
        val json = encode(BACKUP)

        listOf("uid", "name", "enabled", "colorArgb").forEach { assertTrue(it, json.contains("\"$it\"")) }
        listOf("text", "caseSensitive", "color", "searchGroupId").forEach {
            assertTrue(it, json.contains("\"$it\""))
        }
    }

    @Test
    fun `encode writes the app settings`() {
        assertTrue(encode(BACKUP).contains("\"global_default_indicator_color\": \"#FF1B4D9B\""))
    }

    @Test
    fun `decode of a backup without app settings gives an empty map`() {
        val decoded = decode("""{ "searchGroups": [], "searchSettings": [] }""")

        assertEquals(emptyMap<String, String>(), decoded.appSettings)
    }

    @Test
    fun `encode then decode round trips unchanged`() {
        assertEquals(BACKUP, decode(encode(BACKUP)))
    }

    @Test
    fun `encode then decode preserves nulls`() {
        val withNulls = BACKUP.copy(
            searchSettings = listOf(SETTING.copy(color = null, searchGroupId = null)),
        )

        val setting = decode(encode(withNulls)).searchSettings.single()

        assertEquals(null, setting.color)
        assertEquals(null, setting.searchGroupId)
    }

    @Test
    fun `encode then decode round trips empty collections`() {
        val empty = BackupData(searchGroups = emptyList(), searchSettings = emptyList())

        assertEquals(empty, decode(encode(empty)))
    }

    @Test
    fun `decode ignores unknown keys from a newer backup format`() {
        val futureJson = """
            {
              "version": 1,
              "unknownTopLevelField": "ignored",
              "searchGroups": [
                { "uid": 1, "name": "group", "enabled": true, "colorArgb": 255, "futureField": 7 }
              ],
              "searchSettings": [
                {
                  "uid": 10, "text": "needle", "caseSensitive": false, "color": null,
                  "enabled": true, "searchGroupId": 1, "futureField": "x"
                }
              ]
            }
        """.trimIndent()

        val decoded = decode(futureJson)

        assertEquals(listOf("group"), decoded.searchGroups.map { it.name })
        assertEquals(listOf("needle"), decoded.searchSettings.map { it.text })
    }

    @Test
    fun `decode accepts an older backup written without a version field`() {
        val versionlessJson = """
            {
              "searchGroups": [
                { "uid": 1, "name": "group", "enabled": true, "colorArgb": 255 }
              ],
              "searchSettings": []
            }
        """.trimIndent()

        val decoded = decode(versionlessJson)

        assertEquals(BackupData.CURRENT_BACKUP_VERSION, decoded.version)
        assertEquals(listOf("group"), decoded.searchGroups.map { it.name })
    }

    @Test
    fun `decode restores every field`() {
        val decoded = decode(encode(BACKUP))

        val group = decoded.searchGroups.single()
        assertEquals(1L, group.uid)
        assertEquals("group", group.name)
        assertEquals(true, group.enabled)
        assertEquals(0x11223344, group.colorArgb)

        val setting = decoded.searchSettings.single()
        assertEquals(10L, setting.uid)
        assertEquals("needle", setting.text)
        assertEquals(true, setting.caseSensitive)
        assertEquals(0x556677, setting.color)
        assertEquals(false, setting.enabled)
        assertEquals(1L, setting.searchGroupId)
    }

    @Test
    fun `decode of malformed json returns the failure rather than throwing`() {
        val result = serializer.decode("not json at all")

        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is SerializationException)
    }

    @Test
    fun `decode of json missing a required field returns the failure`() {
        val result = serializer.decode("""{ "version": 1, "searchGroups": [] }""")

        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is SerializationException)
    }

    @Test
    fun `decode failure keeps the backup contents out of the log`() {
        // Canary: the log is what a bug report sends, and a backup holds the user's search terms.
        val log = StringBuilder()
        Logger.setLogWriters(
            object : LogWriter() {
                // Mirrors RollingFileLogWriter, which writes the message and the full stack trace.
                override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                    log.appendLine(message)
                    throwable?.let { log.appendLine(it.stackTraceToString()) }
                }
            },
        )

        val result = serializer.decode("""{ "searchSettings": [ { "text": "Jane Canary 555-0100" oops } ] }""")

        assertTrue(result.isLeft())
        assertTrue(log.toString(), log.contains("Backup failed while decoding"))
        assertFalse(log.toString(), log.contains("Canary"))
    }

    private companion object {
        val SETTING = BackupSearchSetting(
            uid = 10L,
            text = "needle",
            caseSensitive = true,
            color = 0x556677,
            enabled = false,
            searchGroupId = 1L,
        )

        val BACKUP = BackupData(
            searchGroups = listOf(BackupSearchGroup(1L, "group", true, 0x11223344)),
            searchSettings = listOf(SETTING),
            appSettings = mapOf("global_default_indicator_color" to "#FF1B4D9B"),
        )
    }
}
