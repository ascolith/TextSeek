package com.ascolith.textseek.core.database.di

import com.ascolith.textseek.core.database.transaction.ITransactionRunner
import com.ascolith.textseek.core.database.transaction.RoomTransactionRunner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface TransactionModule {

    @Binds
    fun bindsTransactionRunner(
        transactionRunner: RoomTransactionRunner,
    ): ITransactionRunner
}
