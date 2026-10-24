package com.ascolith.textseek.core.common.version

import com.ascolith.textseek.domain.feature.version.IAppVersionProvider
import javax.inject.Inject

/**
 * Reads the version from the installed package rather than BuildConfig, which is not generated
 * for the app module and would not be visible from feature modules in any case.
 */
class AppVersionProvider @Inject constructor(
    private val packageInfoProvider: IAppPackageInfoProvider,
) : IAppVersionProvider {

    private val packageInfo by lazy { packageInfoProvider.getAppPackageInfo() }

    override val versionName: String get() = packageInfo.versionName.orEmpty()
    override val versionCode: Long get() = packageInfo.versionCode
}
