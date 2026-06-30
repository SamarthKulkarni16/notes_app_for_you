package com.samarth.notesapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncedEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SyncedEntry)

    @Query("SELECT * FROM synced_entries WHERE filename = :filename LIMIT 1")
    suspend fun getByFilename(filename: String): SyncedEntry?

    @Query("SELECT * FROM synced_entries WHERE synced = 0")
    suspend fun getUnsynced(): List<SyncedEntry>

    @Query("UPDATE synced_entries SET synced = 1, remoteId = :remoteId WHERE filename = :filename")
    suspend fun markSynced(filename: String, remoteId: String)

    @Query("UPDATE synced_entries SET lastAttemptMillis = :attemptMillis WHERE filename = :filename")
    suspend fun markAttempted(filename: String, attemptMillis: Long)
}
