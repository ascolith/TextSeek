package com.ascolith.textseek.domain.feature.version

interface IDeviceInfoProvider {
    val androidVersion: String
    val sdkInt: Int
    val deviceModel: String
}
