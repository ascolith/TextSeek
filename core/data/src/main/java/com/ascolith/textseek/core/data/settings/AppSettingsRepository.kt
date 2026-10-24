package com.ascolith.textseek.core.data.settings

import arrow.core.Either
import com.ascolith.textseek.domain.feature.keyvalue.IKeyValueStore
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey
import com.ascolith.textseek.domain.model.AppSetting
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface IAppSettingsRepository {
    /** Emits the value of [setting], or its default when unset or unreadable, then each change. */
    fun <T> observe(setting: AppSetting<T>): Flow<T>

    suspend fun <T> set(setting: AppSetting<T>, value: T): Either<Throwable, Unit>

    /** Every setting in [AppSetting.all], encoded and keyed by [AppSetting.key]. Unset ones hold their default. */
    suspend fun exportAll(): Map<String, String>

    /**
     * Writes every setting in [AppSetting.all] that [values] holds a valid value for, atomically.
     * Unknown keys and invalid values are skipped; settings missing from [values] are left as they are.
     */
    suspend fun importAll(values: Map<String, String>): Either<Throwable, Unit>
}

class DefaultAppSettingsRepository @Inject constructor(
    private val keyValueStore: IKeyValueStore,
) : IAppSettingsRepository {

    override fun <T> observe(setting: AppSetting<T>): Flow<T> =
        keyValueStore.observe(setting.storageKey)
            .map { setting.decodeOrDefault(it) }
            .distinctUntilChanged()

    override suspend fun <T> set(setting: AppSetting<T>, value: T): Either<Throwable, Unit> =
        keyValueStore.set(setting.storageKey, setting.encode(value))

    override suspend fun exportAll(): Map<String, String> =
        AppSetting.all.associate { setting ->
            setting.key to setting.normalize(keyValueStore.get(setting.storageKey))
        }

    override suspend fun importAll(values: Map<String, String>): Either<Throwable, Unit> =
        keyValueStore.edit {
            AppSetting.all.forEach { setting ->
                val normalized = values[setting.key]?.let { setting.normalizeOrNull(it) }
                if (normalized != null) this[setting.storageKey] = normalized
            }
        }

    private val AppSetting<*>.storageKey get() = KeyValueKey.StringKey(key)

    private fun <T> AppSetting<T>.decodeOrDefault(stored: String?): T =
        stored?.let(::decode) ?: defaultValue

    /** Re-encodes [stored] (or the default) so exports always use the canonical format. */
    private fun <T> AppSetting<T>.normalize(stored: String?): String =
        encode(decodeOrDefault(stored))

    private fun <T> AppSetting<T>.normalizeOrNull(stored: String): String? =
        decode(stored)?.let(::encode)
}
