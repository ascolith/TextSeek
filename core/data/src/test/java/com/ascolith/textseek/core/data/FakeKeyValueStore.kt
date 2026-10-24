package com.ascolith.textseek.core.data

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.ascolith.textseek.domain.feature.keyvalue.IKeyValueStore
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory implementation of [IKeyValueStore] for testing.
 */
class FakeKeyValueStore : IKeyValueStore {

    private val data = MutableStateFlow<Map<KeyValueKey<*>, Any>>(emptyMap())

    /** Everything stored, for assertions. */
    val snapshot: Map<KeyValueKey<*>, Any> get() = data.value

    /** When set, every write fails with this and changes nothing. */
    var writeFailure: Throwable? = null

    override fun <T> observe(key: KeyValueKey<T>): Flow<T?> =
        data.map { it.read(key) }.distinctUntilChanged()

    override suspend fun <T> get(key: KeyValueKey<T>): T? = data.value.read(key)

    override suspend fun contains(key: KeyValueKey<*>): Boolean = key in data.value

    override suspend fun <T> set(key: KeyValueKey<T>, value: T): Either<Throwable, Unit> =
        write { it + (key to value as Any) }

    override suspend fun remove(key: KeyValueKey<*>): Either<Throwable, Unit> =
        write { it - key }

    override suspend fun clear(): Either<Throwable, Unit> =
        write { emptyMap() }

    override suspend fun edit(block: suspend IKeyValueStore.Editor.() -> Unit): Either<Throwable, Unit> {
        val editor = MapEditor(data.value.toMutableMap())
        editor.block()
        return write { editor.values }
    }

    private fun write(transform: (Map<KeyValueKey<*>, Any>) -> Map<KeyValueKey<*>, Any>): Either<Throwable, Unit> {
        writeFailure?.let { return it.left() }
        data.update(transform)
        return Unit.right()
    }

    private class MapEditor(val values: MutableMap<KeyValueKey<*>, Any>) : IKeyValueStore.Editor {
        override fun <T> get(key: KeyValueKey<T>): T? = values.read(key)

        override fun <T> set(key: KeyValueKey<T>, value: T) {
            values[key] = value as Any
        }

        override fun remove(key: KeyValueKey<*>) {
            values.remove(key)
        }

        override fun clear() {
            values.clear()
        }
    }

    private companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> Map<KeyValueKey<*>, Any>.read(key: KeyValueKey<T>): T? = this[key] as T?
    }
}
