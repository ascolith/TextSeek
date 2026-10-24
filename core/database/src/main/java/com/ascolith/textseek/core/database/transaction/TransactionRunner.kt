package com.ascolith.textseek.core.database.transaction

import androidx.room.withTransaction
import com.ascolith.textseek.core.database.TextSeekDatabase
import javax.inject.Inject

/** Runs a block of DAO work atomically. */
interface ITransactionRunner {
    suspend fun <T> withTransaction(block: suspend () -> T): T
}

class RoomTransactionRunner @Inject constructor(
    private val textSeekDatabase: TextSeekDatabase,
) : ITransactionRunner {

    override suspend fun <T> withTransaction(block: suspend () -> T): T =
        textSeekDatabase.withTransaction(block)
}
