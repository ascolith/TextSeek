package com.ascolith.textseek.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSettingTest {

    @Test
    fun `all lists every setting, so none can be left out of backups`() {
        val declared = AppSetting::class.sealedSubclasses.map { it.objectInstance }

        assertEquals(
            "Every AppSetting must be added to AppSetting.all",
            declared.toSet(),
            AppSetting.all.toSet(),
        )
    }

    @Test
    fun `keys are unique`() {
        val keys = AppSetting.all.map { it.key }

        assertEquals(keys.distinct(), keys)
    }

    @Test
    fun `every default value round-trips through encode and decode`() {
        AppSetting.all.forEach { setting ->
            assertEquals(setting.key, setting.defaultValue, setting.roundTrip(setting.defaultValue))
        }
    }

    @Test
    fun `global default indicator color is stored as upper-case ARGB hex`() {
        assertEquals("#FFFFFF00", AppSetting.GlobalDefaultIndicatorColor.encode(0xFFFFFF00.toInt()))
        assertEquals("#0000FF00", AppSetting.GlobalDefaultIndicatorColor.encode(0x0000FF00))
    }

    @Test
    fun `global default indicator color decodes with or without the hash, in either case`() {
        assertEquals(0xFF1B4D9B.toInt(), AppSetting.GlobalDefaultIndicatorColor.decode("#FF1B4D9B"))
        assertEquals(0xFF1B4D9B.toInt(), AppSetting.GlobalDefaultIndicatorColor.decode("ff1b4d9b"))
    }

    @Test
    fun `global default indicator color rejects values that are not eight hex digits`() {
        listOf("", "#", "#FFF", "#FFFFFF", "#FFFFFFFFF", "#GGGGGGGG", "#+FFFFFFF").forEach {
            assertNull(it, AppSetting.GlobalDefaultIndicatorColor.decode(it))
        }
    }

    @Test
    fun `text recognition script defaults to latin`() {
        assertEquals(TextRecognitionScript.LATIN, AppSetting.TextRecognitionScript.defaultValue)
    }

    @Test
    fun `every text recognition script round-trips through encode and decode`() {
        TextRecognitionScript.entries.forEach {
            assertEquals(it, AppSetting.TextRecognitionScript.roundTrip(it))
        }
    }

    @Test
    fun `text recognition script rejects unknown names`() {
        listOf("", "latin", "ARABIC").forEach {
            assertNull(it, AppSetting.TextRecognitionScript.decode(it))
        }
    }

    private fun <T> AppSetting<T>.roundTrip(value: Any?): T? {
        @Suppress("UNCHECKED_CAST")
        return decode(encode(value as T))
    }
}
