package com.ascolith.textseek.core.data.di

import android.content.Context
import co.touchlab.kermit.io.RollingFileLogWriter
import co.touchlab.kermit.io.RollingFileLogWriterConfig
import com.ascolith.textseek.core.data.log.DefaultCrashRepository
import com.ascolith.textseek.core.data.log.DefaultLogRepository
import com.ascolith.textseek.core.data.log.ICrashRepository
import com.ascolith.textseek.core.data.log.ILogRepository
import com.ascolith.textseek.core.data.log.LogFileLocation
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.io.files.Path

@Module
@InstallIn(SingletonComponent::class)
interface LoggingModule {

    @Singleton
    @Binds
    fun bindsLogRepository(
        logRepository: DefaultLogRepository,
    ): ILogRepository

    @Binds
    fun bindsCrashRepository(
        crashRepository: DefaultCrashRepository,
    ): ICrashRepository

    companion object {
        private const val LOG_DIRECTORY_NAME = "logs"
        private const val LOG_FILE_NAME = "text-seek"
        private const val MAX_LOG_FILES = 4
        private const val ROLL_ON_SIZE_BYTES = 256L * 1024

        // filesDir rather than cacheDir: the OS may clear the cache before a bug report is filed.
        @Singleton
        @Provides
        fun provideLogFileLocation(
            @ApplicationContext context: Context,
        ): LogFileLocation = LogFileLocation(
            // Kermit's writer does not create the directory itself.
            directory = File(context.filesDir, LOG_DIRECTORY_NAME).apply { mkdirs() },
            name = LOG_FILE_NAME,
            maxLogFiles = MAX_LOG_FILES,
        )

        // Singleton: each instance owns a writer thread and an open sink on the same file.
        @Singleton
        @Provides
        fun provideRollingFileLogWriter(
            location: LogFileLocation,
        ): RollingFileLogWriter = RollingFileLogWriter(
            RollingFileLogWriterConfig(
                logFileName = location.name,
                logFilePath = Path(location.directory.path),
                rollOnSize = ROLL_ON_SIZE_BYTES,
                maxLogFiles = location.maxLogFiles,
            ),
        )
    }
}
