package com.ascolith.textseek.core.data.di

import com.ascolith.textseek.core.data.onboarding.DefaultOnboardingRepository
import com.ascolith.textseek.core.data.onboarding.IOnboardingRepository
import com.ascolith.textseek.core.data.searchgroup.DefaultSearchGroupRepository
import com.ascolith.textseek.core.data.searchgroup.ISearchGroupRepository
import com.ascolith.textseek.core.data.searchsetting.DefaultSearchSettingRepository
import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.core.data.settings.DefaultAppSettingsRepository
import com.ascolith.textseek.core.data.settings.IAppSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Singleton
    @Binds
    fun bindsSearchSettingRepository(
        searchSettingRepository: DefaultSearchSettingRepository,
    ): ISearchSettingRepository

    @Singleton
    @Binds
    fun bindsSearchGroupRepository(
        searchGroupRepository: DefaultSearchGroupRepository,
    ): ISearchGroupRepository

    @Singleton
    @Binds
    fun bindsAppSettingsRepository(
        appSettingsRepository: DefaultAppSettingsRepository,
    ): IAppSettingsRepository

    @Singleton
    @Binds
    fun bindsOnboardingRepository(
        onboardingRepository: DefaultOnboardingRepository,
    ): IOnboardingRepository
}
