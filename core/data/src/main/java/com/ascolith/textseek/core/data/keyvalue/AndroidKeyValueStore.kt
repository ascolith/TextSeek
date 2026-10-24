package com.ascolith.textseek.core.data.keyvalue

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.touchlab.kermit.Logger
import com.ascolith.textseek.domain.feature.keyvalue.IKeyValueStore
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AndroidKeyValueStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : IKeyValueStore {

    private val data: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) {
            Logger.e(e) { "Failed to read the key-value store; treating it as empty" }
            emit(emptyPreferences())
        } else {
            throw e
        }
    }

    override fun <T> observe(key: KeyValueKey<T>): Flow<T?> {
        val preferencesKey = key.toPreferencesKey()
        return data.map { it[preferencesKey] }.distinctUntilChanged()
    }

    override suspend fun <T> get(key: KeyValueKey<T>): T? =
        data.first()[key.toPreferencesKey()]

    override suspend fun contains(key: KeyValueKey<*>): Boolean =
        key.toPreferencesKey() in data.first()

    override suspend fun <T> set(key: KeyValueKey<T>, value: T): Either<Throwable, Unit> =
        write { it[key.toPreferencesKey()] = value }

    override suspend fun remove(key: KeyValueKey<*>): Either<Throwable, Unit> =
        write { it.remove(key.toPreferencesKey()) }

    override suspend fun clear(): Either<Throwable, Unit> =
        write { it.clear() }

    override suspend fun edit(block: suspend IKeyValueStore.Editor.() -> Unit): Either<Throwable, Unit> =
        write { PreferencesEditor(it).block() }

    /** DataStore reports a failed write by throwing; this turns that into the interface's Either. */
    private suspend fun write(transform: suspend (MutablePreferences) -> Unit): Either<Throwable, Unit> =
        try {
            dataStore.edit(transform)
            Unit.right()
        } catch (e: IOException) {
            Logger.e(e) { "Failed to write to the key-value store" }
            e.left()
        }

    private class PreferencesEditor(
        private val preferences: MutablePreferences,
    ) : IKeyValueStore.Editor {
        override fun <T> get(key: KeyValueKey<T>): T? = preferences[key.toPreferencesKey()]

        override fun <T> set(key: KeyValueKey<T>, value: T) {
            preferences[key.toPreferencesKey()] = value
        }

        override fun remove(key: KeyValueKey<*>) {
            preferences.remove(key.toPreferencesKey())
        }

        override fun clear() {
            preferences.clear()
        }
    }
}

// Each branch builds the key whose type parameter matches the subclass's, so the cast is safe.
@Suppress("UNCHECKED_CAST")
private fun <T> KeyValueKey<T>.toPreferencesKey(): Preferences.Key<T> = when (this) {
    is KeyValueKey.BooleanKey -> booleanPreferencesKey(name)
    is KeyValueKey.IntKey -> intPreferencesKey(name)
    is KeyValueKey.LongKey -> longPreferencesKey(name)
    is KeyValueKey.FloatKey -> floatPreferencesKey(name)
    is KeyValueKey.DoubleKey -> doublePreferencesKey(name)
    is KeyValueKey.StringKey -> stringPreferencesKey(name)
    is KeyValueKey.StringSetKey -> stringSetPreferencesKey(name)
    is KeyValueKey.ByteArrayKey -> byteArrayPreferencesKey(name)
} as Preferences.Key<T>
