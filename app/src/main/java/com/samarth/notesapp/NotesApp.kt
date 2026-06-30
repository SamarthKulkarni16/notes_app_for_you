package com.samarth.notesapp

import android.app.Application
import com.samarth.notesapp.data.local.AppDatabase
import com.samarth.notesapp.data.local.EntryFileStore
import com.samarth.notesapp.data.remote.NotesRepository
import com.samarth.notesapp.sync.ConnectivityObserver
import com.samarth.notesapp.sync.SyncManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Holds the app's shared singletons for its whole lifetime: the Supabase
 * client, the local .md file store, the Room sync ledger, and the sync
 * manager that ties them together.
 *
 * SUPABASE_URL / SUPABASE_ANON_KEY come from BuildConfig, which is populated
 * at build time from gradle properties (local) or CI secrets (release builds).
 * The anon key is safe to ship in the compiled app — it is not a secret on
 * its own; access control is enforced by Postgres Row Level Security (RLS)
 * policies on the `notes` table, not by hiding this key.
 */
class NotesApp : Application() {

    lateinit var supabase: SupabaseClient
        private set

    lateinit var entryFileStore: EntryFileStore
        private set

    lateinit var syncManager: SyncManager
        private set

    lateinit var connectivityObserver: ConnectivityObserver
        private set

    override fun onCreate() {
        super.onCreate()

        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }

        entryFileStore = EntryFileStore(this)
        connectivityObserver = ConnectivityObserver(this)

        val database = AppDatabase.getInstance(this)
        val notesRepository = NotesRepository(supabase)
        syncManager = SyncManager(
            fileStore = entryFileStore,
            syncedEntryDao = database.syncedEntryDao(),
            notesRepository = notesRepository
        )
    }
}
