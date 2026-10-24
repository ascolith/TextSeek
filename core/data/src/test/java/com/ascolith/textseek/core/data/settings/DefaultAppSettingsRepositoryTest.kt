package com.ascolith.textseek.core.data.settings

import com.ascolith.textseek.core.data.FakeKeyValueStore
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey
import com.ascolith.textseek.domain.model.AppSetting
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import java.io.IOException
import org.junit.Test

class DefaultAppSettingsRepositoryTest {

    private val keyValueStore = FakeKeyValueStore()
    private val repository = DefaultAppSettingsRepository(keyValueStore)

    @Test
    fun `observe emits the default when the setting was never set`() = runTest {
        assertEquals(COLOR_SETTING.defaultValue, repository.observe(COLOR_SETTING).first())
    }

    @Test
    fun `observe emits the value that was set`() = runTest {
        repository.set(AppSetting.GlobalDefaultIndicatorColor, BLUE)

        assertEquals(BLUE, repository.observe(AppSetting.GlobalDefaultIndicatorColor).first())
    }

    @Test
    fun `set stores the global default indicator color as hex`() = runTest {
        repository.set(AppSetting.GlobalDefaultIndicatorColor, BLUE)

        assertEquals("#FF1B4D9B", keyValueStore.get(GLOBAL_DEFAULT_INDICATOR_COLOR_KEY))
    }

    @Test
    fun `observe falls back to the default when the stored value is invalid`() = runTest {
        keyValueStore.set(GLOBAL_DEFAULT_INDICATOR_COLOR_KEY, "not a color")

        assertEquals(COLOR_SETTING.defaultValue, repository.observe(COLOR_SETTING).first())
    }

    @Test
    fun `exportAll includes every setting, with defaults for unset ones`() = runTest {
        val exported = repository.exportAll()

        assertEquals(AppSetting.all.map { it.key }.toSet(), exported.keys)
        assertEquals(COLOR_SETTING.encode(COLOR_SETTING.defaultValue), exported[COLOR_SETTING.key])
    }

    @Test
    fun `exportAll includes values that were set`() = runTest {
        repository.set(AppSetting.GlobalDefaultIndicatorColor, BLUE)

        assertEquals("#FF1B4D9B", repository.exportAll()["global_default_indicator_color"])
    }

    @Test
    fun `importAll writes valid values in the canonical format`() = runTest {
        repository.importAll(mapOf("global_default_indicator_color" to "ff1b4d9b"))

        assertEquals(BLUE, repository.observe(AppSetting.GlobalDefaultIndicatorColor).first())
        assertEquals("#FF1B4D9B", keyValueStore.get(GLOBAL_DEFAULT_INDICATOR_COLOR_KEY))
    }

    @Test
    fun `importAll skips invalid values, keeping the current one`() = runTest {
        repository.set(AppSetting.GlobalDefaultIndicatorColor, BLUE)

        repository.importAll(mapOf("global_default_indicator_color" to "#nope"))

        assertEquals(BLUE, repository.observe(AppSetting.GlobalDefaultIndicatorColor).first())
    }

    @Test
    fun `importAll ignores unknown keys`() = runTest {
        repository.importAll(mapOf("from_a_newer_version" to "value"))

        assertFalse(keyValueStore.contains(KeyValueKey.StringKey("from_a_newer_version")))
    }

    @Test
    fun `importAll leaves settings missing from the backup as they are`() = runTest {
        repository.set(AppSetting.GlobalDefaultIndicatorColor, BLUE)

        repository.importAll(emptyMap())

        assertEquals(BLUE, repository.observe(AppSetting.GlobalDefaultIndicatorColor).first())
    }

    @Test
    fun `set reports a write failure instead of throwing`() = runTest {
        val failure = IOException("disk full")
        keyValueStore.writeFailure = failure

        val result = repository.set(COLOR_SETTING, BLUE)

        assertSame(failure, result.leftOrNull())
        assertEquals(COLOR_SETTING.defaultValue, repository.observe(COLOR_SETTING).first())
    }

    @Test
    fun `importAll reports a write failure instead of throwing`() = runTest {
        keyValueStore.writeFailure = IOException("disk full")

        val result = repository.importAll(mapOf(COLOR_SETTING.key to "#FF1B4D9B"))

        assertTrue(result.isLeft())
        assertEquals(COLOR_SETTING.defaultValue, repository.observe(COLOR_SETTING).first())
    }

    @Test
    fun `exportAll then importAll round trips`() = runTest {
        repository.set(AppSetting.GlobalDefaultIndicatorColor, BLUE)
        val exported = repository.exportAll()
        val restored = DefaultAppSettingsRepository(FakeKeyValueStore())

        restored.importAll(exported)

        assertEquals(exported, restored.exportAll())
    }

    private companion object {
        val COLOR_SETTING = AppSetting.GlobalDefaultIndicatorColor
        val BLUE = 0xFF1B4D9B.toInt()
        val GLOBAL_DEFAULT_INDICATOR_COLOR_KEY = KeyValueKey.StringKey("global_default_indicator_color")
    }
}
