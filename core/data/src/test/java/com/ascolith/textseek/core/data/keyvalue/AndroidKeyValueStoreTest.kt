package com.ascolith.textseek.core.data.keyvalue

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey.BooleanKey
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey.ByteArrayKey
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey.DoubleKey
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey.FloatKey
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey.IntKey
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey.LongKey
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey.StringKey
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey.StringSetKey
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidKeyValueStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun TestScope.createStore(): AndroidKeyValueStore = AndroidKeyValueStore(
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFolder.root.resolve("test.preferences_pb") },
        ),
    )

    @Test
    fun `get returns null for a key that was never set`() = runTest {
        val store = createStore()

        assertNull(store.get(StringKey("missing")))
    }

    @Test
    fun `set then get round-trips every key type`() = runTest {
        val store = createStore()

        store.set(BooleanKey("boolean"), true)
        store.set(IntKey("int"), 42)
        store.set(LongKey("long"), 42L)
        store.set(FloatKey("float"), 4.2f)
        store.set(DoubleKey("double"), 4.2)
        store.set(StringKey("string"), "hello")
        store.set(StringSetKey("stringSet"), setOf("a", "b"))
        store.set(ByteArrayKey("byteArray"), byteArrayOf(1, 2, 3))

        assertEquals(true, store.get(BooleanKey("boolean")))
        assertEquals(42, store.get(IntKey("int")))
        assertEquals(42L, store.get(LongKey("long")))
        assertEquals(4.2f, store.get(FloatKey("float")))
        assertEquals(4.2, store.get(DoubleKey("double")))
        assertEquals("hello", store.get(StringKey("string")))
        assertEquals(setOf("a", "b"), store.get(StringSetKey("stringSet")))
        assertArrayEquals(byteArrayOf(1, 2, 3), store.get(ByteArrayKey("byteArray")))
    }

    @Test
    fun `set overwrites the previous value`() = runTest {
        val store = createStore()

        store.set(IntKey("count"), 1)
        store.set(IntKey("count"), 2)

        assertEquals(2, store.get(IntKey("count")))
    }

    @Test
    fun `contains reflects whether the key has been set`() = runTest {
        val store = createStore()
        val key = BooleanKey("flag")

        assertFalse(store.contains(key))
        store.set(key, false)
        assertTrue(store.contains(key))
    }

    @Test
    fun `remove removes only the given key`() = runTest {
        val store = createStore()
        store.set(StringKey("a"), "1")
        store.set(StringKey("b"), "2")

        store.remove(StringKey("a"))

        assertNull(store.get(StringKey("a")))
        assertEquals("2", store.get(StringKey("b")))
    }

    @Test
    fun `clear removes every key`() = runTest {
        val store = createStore()
        store.set(StringKey("a"), "1")
        store.set(IntKey("b"), 2)

        store.clear()

        assertFalse(store.contains(StringKey("a")))
        assertFalse(store.contains(IntKey("b")))
    }

    @Test
    fun `observe emits the current value and each distinct change`() = runTest {
        val store = createStore()
        val key = StringKey("observed")
        val emissions = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.observe(key).collect { emissions += it }
        }
        runCurrent()

        store.set(key, "first")
        runCurrent()
        store.set(key, "first")
        runCurrent()
        store.set(StringKey("unrelated"), "ignored")
        runCurrent()
        store.remove(key)
        runCurrent()

        assertEquals(listOf(null, "first", null), emissions)
    }

    @Test
    fun `edit applies several changes together`() = runTest {
        val store = createStore()
        store.set(StringKey("stale"), "old")

        store.edit {
            this[StringKey("name")] = "grep"
            this[BooleanKey("enabled")] = true
            remove(StringKey("stale"))
        }

        assertEquals("grep", store.get(StringKey("name")))
        assertEquals(true, store.get(BooleanKey("enabled")))
        assertNull(store.get(StringKey("stale")))
    }

    @Test
    fun `edit supports read-modify-write`() = runTest {
        val store = createStore()
        val counter = IntKey("counter")
        store.set(counter, 1)

        store.edit { this[counter] = (this[counter] ?: 0) + 1 }

        assertEquals(2, store.get(counter))
    }

    @Test
    fun `edit sees its own earlier changes, including clear`() = runTest {
        val store = createStore()
        store.set(StringKey("a"), "1")

        store.edit {
            clear()
            assertNull(this[StringKey("a")])
            this[StringKey("b")] = "2"
            assertEquals("2", this[StringKey("b")])
        }

        assertNull(store.get(StringKey("a")))
        assertEquals("2", store.get(StringKey("b")))
    }

    @Test
    fun `writes return Right when they succeed`() = runTest {
        val store = createStore()

        assertTrue(store.set(StringKey("a"), "1").isRight())
        assertTrue(store.edit { this[StringKey("b")] = "2" }.isRight())
        assertTrue(store.remove(StringKey("a")).isRight())
        assertTrue(store.clear().isRight())
    }

    @Test
    fun `a failed write is returned as Left rather than thrown`() = runTest {
        val failure = IOException("disk full")
        val store = AndroidKeyValueStore(FailingDataStore(failure))

        assertSame(failure, store.set(StringKey("a"), "1").leftOrNull())
        assertSame(failure, store.remove(StringKey("a")).leftOrNull())
        assertSame(failure, store.clear().leftOrNull())
        assertSame(failure, store.edit { this[StringKey("a")] = "1" }.leftOrNull())
    }

    @Test
    fun `an unreadable store reads as empty`() = runTest {
        val store = AndroidKeyValueStore(FailingDataStore(IOException("unreadable")))

        assertNull(store.get(StringKey("a")))
        assertFalse(store.contains(StringKey("a")))
    }

    @Test
    fun `keys are equal only when both name and type match`() {
        assertEquals(IntKey("a"), IntKey("a"))
        assertEquals(IntKey("a").hashCode(), IntKey("a").hashCode())
        assertNotEquals(IntKey("a"), IntKey("b"))
        assertNotEquals(IntKey("a"), LongKey("a"))
    }

    /** Fails every read and write with [failure], as DataStore does when its file cannot be used. */
    private class FailingDataStore(private val failure: IOException) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw failure }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            throw failure
    }
}
