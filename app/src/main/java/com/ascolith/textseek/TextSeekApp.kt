package com.ascolith.textseek

import android.app.Application
import co.touchlab.kermit.Logger
import com.ascolith.textseek.core.data.log.CrashRecorder
import com.ascolith.textseek.core.data.log.FileLoggingInitializer
import com.ascolith.textseek.domain.feature.version.IAppVersionProvider
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TextSeekApp : Application() {

    @Inject
    lateinit var fileLoggingInitializer: FileLoggingInitializer

    @Inject
    lateinit var crashRecorder: CrashRecorder

    @Inject
    lateinit var appVersionProvider: IAppVersionProvider

    override fun onCreate() {
        super.onCreate() // Hilt injects fields here
        fileLoggingInitializer.install()
        crashRecorder.install()
        Logger.i { "Starting $packageName ${appVersionProvider.versionName} (${appVersionProvider.versionCode})" }
    }
}
