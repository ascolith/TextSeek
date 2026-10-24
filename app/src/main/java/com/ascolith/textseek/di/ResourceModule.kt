package com.ascolith.textseek.di

import com.ascolith.textseek.core.stringprovider.AndroidStringProvider
import com.ascolith.textseek.domain.feature.stringprovider.IStringProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ResourceModule {
    @Binds
    abstract fun bindStringProvider(
        androidStringProvider: AndroidStringProvider,
    ): IStringProvider
}
