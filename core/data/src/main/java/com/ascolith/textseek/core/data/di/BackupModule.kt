package com.ascolith.textseek.core.data.di

import com.ascolith.textseek.core.data.backup.BackupApplier
import com.ascolith.textseek.core.data.backup.BackupCollector
import com.ascolith.textseek.core.data.backup.BackupSerializer
import com.ascolith.textseek.core.data.backup.DefaultBackupRepository
import com.ascolith.textseek.core.data.backup.IBackupApplier
import com.ascolith.textseek.core.data.backup.IBackupCollector
import com.ascolith.textseek.core.data.backup.IBackupRepository
import com.ascolith.textseek.core.data.backup.IBackupSerializer
import com.ascolith.textseek.core.data.file.CacheFileWriter
import com.ascolith.textseek.core.data.file.ICacheFileWriter
import com.ascolith.textseek.core.data.file.IUriContentReader
import com.ascolith.textseek.core.data.file.UriContentReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface BackupModule {

    @Singleton
    @Binds
    fun bindsBackupRepository(
        backupRepository: DefaultBackupRepository,
    ): IBackupRepository

    @Binds
    fun bindsBackupCollector(
        backupCollector: BackupCollector,
    ): IBackupCollector

    @Binds
    fun bindsBackupApplier(
        backupApplier: BackupApplier,
    ): IBackupApplier

    @Binds
    fun bindsBackupSerializer(
        backupSerializer: BackupSerializer,
    ): IBackupSerializer

    @Binds
    fun bindsCacheFileWriter(
        cacheFileWriter: CacheFileWriter,
    ): ICacheFileWriter

    @Binds
    fun bindsUriContentReader(
        uriContentReader: UriContentReader,
    ): IUriContentReader
}
