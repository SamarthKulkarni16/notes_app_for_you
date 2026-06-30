package com.samarth.notesapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sync ledger row — one per .md filename. This table holds NO journal
 * content; the .md file in EntryFileStore is the single source of truth
 * for what was written. This table only tracks "has this file made it to
 * Supabase yet," so a background worker knows what still needs pushing.
 */
@Entity(tableName = "synced_entries")
data class SyncedEntry(
    @PrimaryKey val filename: String,
    val synced: Boolean,
    val remoteId: String? = null,
    val lastAttemptMillis: Long = 0L
)
