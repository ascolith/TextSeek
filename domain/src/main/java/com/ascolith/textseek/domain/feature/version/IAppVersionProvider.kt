package com.ascolith.textseek.domain.feature.version

interface IAppVersionProvider {
    val versionName: String
    val versionCode: Long
}
