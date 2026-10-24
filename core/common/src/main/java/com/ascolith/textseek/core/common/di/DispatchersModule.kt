package com.ascolith.textseek.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Dispatcher(TsDispatcher.IO)
    fun provideIODispatcher(): CoroutineContext = Dispatchers.IO

    @Provides
    @Dispatcher(TsDispatcher.Default)
    fun provideDefaultDispatcher(): CoroutineContext = Dispatchers.Default
}
