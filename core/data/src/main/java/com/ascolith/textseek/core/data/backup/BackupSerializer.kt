package com.ascolith.textseek.core.data.backup

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.touchlab.kermit.Logger
import com.ascolith.textseek.core.data.backup.model.BackupData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface IBackupSerializer {
    fun encode(backupData: BackupData): Either<Throwable, String>

    fun decode(json: String): Either<Throwable, BackupData>
}

class BackupSerializer @Inject constructor() : IBackupSerializer {

    private val jsonFormat = Json {
        prettyPrint = true
        // Accept fields a newer version of the app might add, so a backup is not one-way.
        ignoreUnknownKeys = true
        // BackupData.version defaults to [CURRENT_BACKUP_VERSION]. This ensures that value is written.
        encodeDefaults = true
    }

    override fun encode(backupData: BackupData): Either<Throwable, String> =
        try {
            jsonFormat.encodeToString(BackupData.serializer(), backupData).right()
        } catch (e: SerializationException) {
            Logger.e(e) { "Backup failed while encoding" }
            e.left()
        }

    override fun decode(json: String): Either<Throwable, BackupData> =
        try {
            jsonFormat.decodeFromString(BackupData.serializer(), json).right()
        } catch (e: IllegalArgumentException) {
            // Type only: kotlinx.serialization quotes the JSON input, which holds the user's
            // search terms, in the exception message.
            Logger.e { "Backup failed while decoding: ${e::class.simpleName}" }
            e.left()
        }
}
