package com.ascolith.textseek.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExecutorsModule {

    @Provides
    @Singleton
    fun provideCameraExecutor(): Executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "camera-analysis")
    }
}
