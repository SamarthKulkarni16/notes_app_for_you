package com.samarth.notesapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the `notes` table created in Phase 2:
 *   id uuid primary key default gen_random_uuid()
 *   user_id uuid not null references auth.users(id)
 *   body text not null
 *   created_at timestamptz not null default now()
 *
 * `id` and `createdAt` are nullable on the way IN (insert) since Postgres
 * fills in defaults server-side — we only send user_id and body on insert,
 * then decode the full row (including server-assigned id) from the response.
 */
@Serializable
data class NoteRow(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String? = null
)
