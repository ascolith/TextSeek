package com.ascolith.textseek.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ascolith.textseek.core.common.di.Dispatcher
import com.ascolith.textseek.core.common.di.TsDispatcher
import com.ascolith.textseek.core.data.keyvalue.AndroidKeyValueStore
import com.ascolith.textseek.domain.feature.keyvalue.IKeyValueStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
interface KeyValueStoreModule {

    @Singleton
    @Binds
    fun bindsKeyValueStore(
        keyValueStore: AndroidKeyValueStore,
    ): IKeyValueStore

    companion object {
        private const val PREFERENCES_FILE_NAME = "text_seek_preferences"

        // Singleton: DataStore forbids more than one active instance per file in a process.
        @Singleton
        @Provides
        fun providePreferencesDataStore(
            @ApplicationContext context: Context,
            @Dispatcher(TsDispatcher.IO) ioDispatcher: CoroutineContext,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile(PREFERENCES_FILE_NAME) },
        )
    }
}
