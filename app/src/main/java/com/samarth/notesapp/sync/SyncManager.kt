package com.samarth.notesapp.sync

import com.samarth.notesapp.data.local.EntryFileStore
import com.samarth.notesapp.data.local.SyncedEntry
import com.samarth.notesapp.data.local.SyncedEntryDao
import com.samarth.notesapp.data.remote.NotesRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pushes any not-yet-synced .md files to the Supabase `notes` table.
 *
 * The .md file is always written first (instant, no network dependency);
 * this class is only ever called afterward, opportunistically, to catch
 * the local file up to the cloud. If it fails (offline, transient error),
 * the entry simply stays marked unsynced and gets retried on the next
 * call — there is no data loss, since the .md file itself is unaffected.
 */
class SyncManager(
    private val fileStore: EntryFileStore,
    private val syncedEntryDao: SyncedEntryDao,
    private val notesRepository: NotesRepository
) {
    // Prevents two overlapping sync passes (e.g. one triggered by a save,
    // another by connectivity regained) from racing each other.
    private val syncMutex = Mutex()

    /**
     * Registers a brand-new local entry in the sync ledger as unsynced,
     * then immediately attempts to push it. Safe to call even if offline —
     * the attempt will simply fail and retry later via [syncPendingEntries].
     *
     * Returns true only if *this* filename specifically ended up synced —
     * used by the write screen's status indicator. A false return does not
     * mean data loss: the .md file is untouched and will retry later.
     */
    suspend fun registerAndSync(filename: String, userId: String): Boolean {
        syncedEntryDao.upsert(SyncedEntry(filename = filename, synced = false))
        syncPendingEntries(userId)
        return syncedEntryDao.getByFilename(filename)?.synced == true
    }

    /** Retries every not-yet-synced entry. Call on app start and on regaining connectivity. */
    suspend fun syncPendingEntries(userId: String) = syncMutex.withLock {
        val unsynced = syncedEntryDao.getUnsynced()
        for (entry in unsynced) {
            val body = fileStore.readBody(entry.filename) ?: continue
            if (body.isBlank()) continue // never sync empty drafts

            runCatching {
                val remoteRow = notesRepository.insertNote(userId = userId, body = body)
                val remoteId = remoteRow.id
                if (remoteId != null) {
                    syncedEntryDao.markSynced(entry.filename, remoteId)
                }
            }.onFailure {
                syncedEntryDao.markAttempted(entry.filename, System.currentTimeMillis())
                // Swallow and continue — this entry stays unsynced and will
                // be retried on the next pass. One failure shouldn't block
                // syncing the rest of the queue.
            }
        }
    }
}
