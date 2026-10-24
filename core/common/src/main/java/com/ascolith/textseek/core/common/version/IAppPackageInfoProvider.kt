package com.ascolith.textseek.core.common.version

data class AppPackageInfo(
    val versionName: String?,
    val versionCode: Long,
)

fun interface IAppPackageInfoProvider {
    fun getAppPackageInfo(): AppPackageInfo
}
