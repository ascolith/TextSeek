package com.ascolith.textseek.core.common.di

import android.content.Context
import com.ascolith.textseek.core.common.version.AppPackageInfo
import com.ascolith.textseek.core.common.version.AppVersionProvider
import com.ascolith.textseek.core.common.version.DeviceInfoProvider
import com.ascolith.textseek.core.common.version.IAppPackageInfoProvider
import com.ascolith.textseek.domain.feature.version.IAppVersionProvider
import com.ascolith.textseek.domain.feature.version.IDeviceInfoProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface VersionModule {
    @Binds
    fun bindAppVersionProvider(appVersionProvider: AppVersionProvider): IAppVersionProvider

    @Binds
    fun bindDeviceInfoProvider(deviceInfoProvider: DeviceInfoProvider): IDeviceInfoProvider

    companion object {
        @Provides
        fun provideAppPackageInfoProvider(
            @ApplicationContext context: Context,
        ): IAppPackageInfoProvider = IAppPackageInfoProvider {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            AppPackageInfo(
                versionName = packageInfo.versionName,
                versionCode = packageInfo.longVersionCode,
            )
        }
    }
}
