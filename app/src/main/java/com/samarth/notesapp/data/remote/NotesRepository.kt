package com.samarth.notesapp.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

/**
 * Talks to the `notes` table created in Phase 2. RLS on that table means
 * every call here is implicitly scoped to whichever user's session is
 * active — there is no need (and no way) to query another user's rows.
 */
class NotesRepository(private val supabase: SupabaseClient) {

    /**
     * Inserts one entry for the given user and returns the server-assigned
     * row (including its generated id and created_at timestamp).
     */
    suspend fun insertNote(userId: String, body: String): NoteRow {
        return supabase.from("notes")
            .insert(NoteRow(userId = userId, body = body)) {
                select()
            }
            .decodeSingle<NoteRow>()
    }
}
