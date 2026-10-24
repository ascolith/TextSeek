package com.ascolith.textseek.core.common.di

import com.ascolith.textseek.domain.feature.time.ITimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.Clock

@Module
@InstallIn(SingletonComponent::class)
interface TimeModule {

    companion object {

        @Provides
        fun provideTimeProvider(): ITimeProvider = { Clock.System.now() }
    }
}
