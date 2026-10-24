package com.ascolith.textseek.core.common.version

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersionProviderTest {

    private var packageInfo = AppPackageInfo(versionName = "1.2.3", versionCode = 42L)
    private var readCount = 0

    private val provider = AppVersionProvider(
        packageInfoProvider = {
            readCount++
            packageInfo
        },
    )

    @Test
    fun `version name comes from the installed package`() {
        assertEquals("1.2.3", provider.versionName)
    }

    @Test
    fun `version name is empty when the package declares none`() {
        packageInfo = packageInfo.copy(versionName = null)

        assertEquals("", provider.versionName)
    }

    @Test
    fun `version code comes from the installed package`() {
        assertEquals(42L, provider.versionCode)
    }

    @Test
    fun `version codes beyond int range are preserved`() {
        val majorVersionCode = 5L shl 32
        packageInfo = packageInfo.copy(versionCode = majorVersionCode)

        assertEquals(majorVersionCode, provider.versionCode)
    }

    @Test
    fun `package info is read once and reused across properties`() {
        // Prime the lazily-read package info, then change what the provider would return.
        assertEquals("1.2.3", provider.versionName)
        packageInfo = AppPackageInfo(versionName = "9.9.9", versionCode = 99L)

        assertEquals("1.2.3", provider.versionName)
        assertEquals(42L, provider.versionCode)
        assertEquals(1, readCount)
    }
}
