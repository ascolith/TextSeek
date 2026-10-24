package com.ascolith.textseek.core.data.onboarding

import arrow.core.Either
import com.ascolith.textseek.domain.feature.keyvalue.IKeyValueStore
import com.ascolith.textseek.domain.feature.keyvalue.KeyValueKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface IOnboardingRepository {
    /** Emits whether onboarding has been completed, then each change. */
    fun observeCompleted(): Flow<Boolean>

    suspend fun markCompleted(): Either<Throwable, Unit>

    /** Makes onboarding show again, as on first launch. */
    suspend fun reset(): Either<Throwable, Unit>
}

class DefaultOnboardingRepository @Inject constructor(
    private val keyValueStore: IKeyValueStore,
) : IOnboardingRepository {

    override fun observeCompleted(): Flow<Boolean> =
        keyValueStore.observe(COMPLETED_KEY)
            .map { it == true }
            .distinctUntilChanged()

    override suspend fun markCompleted(): Either<Throwable, Unit> = keyValueStore.set(COMPLETED_KEY, true)

    override suspend fun reset(): Either<Throwable, Unit> = keyValueStore.remove(COMPLETED_KEY)

    private companion object {
        // Not an AppSetting, so backups never carry it
        val COMPLETED_KEY = KeyValueKey.BooleanKey("onboarding_completed")
    }
}
