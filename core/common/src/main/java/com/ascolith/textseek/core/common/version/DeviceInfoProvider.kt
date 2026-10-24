package com.ascolith.textseek.core.common.version

import android.os.Build
import com.ascolith.textseek.domain.feature.version.IDeviceInfoProvider
import javax.inject.Inject

class DeviceInfoProvider @Inject constructor() : IDeviceInfoProvider {
    override val androidVersion: String get() = Build.VERSION.RELEASE
    override val sdkInt: Int get() = Build.VERSION.SDK_INT
    override val deviceModel: String get() = "${Build.MANUFACTURER} ${Build.MODEL}"
}
