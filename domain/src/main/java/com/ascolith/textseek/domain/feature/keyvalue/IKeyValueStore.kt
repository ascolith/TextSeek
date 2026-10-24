package com.ascolith.textseek.domain.feature.keyvalue

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

/**
 * A typed key into an [IKeyValueStore]. Two keys are equal only when both their [name] and their
 * value type match. Names still share one namespace in storage, though: never reuse a name with a
 * different type, or reads through the other key will fail with a [ClassCastException].
 */
sealed class KeyValueKey<T>(val name: String) {
    class BooleanKey(name: String) : KeyValueKey<Boolean>(name)
    class IntKey(name: String) : KeyValueKey<Int>(name)
    class LongKey(name: String) : KeyValueKey<Long>(name)
    class FloatKey(name: String) : KeyValueKey<Float>(name)
    class DoubleKey(name: String) : KeyValueKey<Double>(name)
    class StringKey(name: String) : KeyValueKey<String>(name)
    class StringSetKey(name: String) : KeyValueKey<Set<String>>(name)
    class ByteArrayKey(name: String) : KeyValueKey<ByteArray>(name)

    override fun equals(other: Any?): Boolean =
        other is KeyValueKey<*> && other.javaClass == javaClass && other.name == name

    override fun hashCode(): Int = 31 * javaClass.hashCode() + name.hashCode()

    override fun toString(): String = "${javaClass.simpleName}($name)"
}

/**
 * Persistent, typed key-value storage for small values such as settings and flags.
 *
 * Reads never fail: if the backing store cannot be read, they behave as if it were empty.
 * Writes return the failure as [Either.Left] if the change cannot be persisted, in which case
 * nothing is changed.
 */
interface IKeyValueStore {
    /** Emits the current value for [key] (null if absent), then each distinct change to it. */
    fun <T> observe(key: KeyValueKey<T>): Flow<T?>

    /** Returns the current value for [key], or null if absent. */
    suspend fun <T> get(key: KeyValueKey<T>): T?

    suspend fun contains(key: KeyValueKey<*>): Boolean

    suspend fun <T> set(key: KeyValueKey<T>, value: T): Either<Throwable, Unit>

    suspend fun remove(key: KeyValueKey<*>): Either<Throwable, Unit>

    /** Removes every key. */
    suspend fun clear(): Either<Throwable, Unit>

    /**
     * Applies every change made in [block] as a single atomic transaction. Values read through the
     * [Editor] reflect the changes already made in [block], so this also serves read-modify-write.
     */
    suspend fun edit(block: suspend Editor.() -> Unit): Either<Throwable, Unit>

    interface Editor {
        operator fun <T> get(key: KeyValueKey<T>): T?
        operator fun <T> set(key: KeyValueKey<T>, value: T)
        fun remove(key: KeyValueKey<*>)
        fun clear()
    }
}
