package com.samarth.notesapp.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A single journal entry as read off disk, with its date/time header parsed out. */
data class EntryFile(
    val filename: String,
    val dateLabel: String,
    val timeLabel: String,
    val body: String,
    val createdAtMillis: Long
)

/**
 * Reads and writes journal entries as .md files in app-private internal
 * storage (context.filesDir/notes/). This directory is NOT a cache — Android
 * never clears it automatically, and "clear cache" in device settings or
 * third-party cache-cleaner apps cannot touch it. Only app uninstall or an
 * explicit "clear app data" removes these files.
 *
 * Filenames follow the same convention as the existing web Notes editor:
 * yyyy-MM-dd-HHmmss.md — so entries sort correctly by filename alone. File
 * *contents* also match the web editor's format exactly:
 *
 *   # June 30, 2026
 *   ### 7:49 AM
 *
 *   {body}
 *
 * The plain-text body (without this header) is what's shown in the write
 * screen's text field and what gets synced to Supabase — the markdown
 * header is purely a local on-disk presentation detail, parsed back out
 * on read.
 */
class EntryFileStore(context: Context) {

    private val notesDir: File = File(context.filesDir, "notes").apply {
        if (!exists()) mkdirs()
    }

    private val filenameFormat = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US)
    private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    /** Builds a fresh filename for a brand-new entry started right now. */
    fun newFilenameForNow(): String = "${filenameFormat.format(Date())}.md"

    /** Human-readable date label for "right now", matching the web editor's format (e.g. "June 30, 2026"). */
    fun currentDateLabel(): String = dateFormat.format(Date())

    /** Human-readable time label for "right now", matching the web editor's format (e.g. "7:49 AM"). */
    fun currentTimeLabel(): String = timeFormat.format(Date())

    /**
     * Overwrites (or creates) the given entry's file with the latest plain-
     * text body, wrapped in the same `# date\n### time\n\n` header the web
     * editor writes. dateLabel/timeLabel are passed in (captured once, at
     * entry-creation time) rather than recomputed here, so the header
     * doesn't drift if autosave fires again a minute later.
     */
    suspend fun write(filename: String, dateLabel: String, timeLabel: String, body: String) =
        withContext(Dispatchers.IO) {
            val content = "# $dateLabel\n### $timeLabel\n\n$body"
            File(notesDir, filename).writeText(content)
        }

    /** Reads back just the plain-text body (header stripped), e.g. for re-syncing. */
    suspend fun readBody(filename: String): String? = withContext(Dispatchers.IO) {
        val file = File(notesDir, filename)
        if (!file.exists()) return@withContext null
        stripHeader(file.readText())
    }

    suspend fun delete(filename: String) = withContext(Dispatchers.IO) {
        File(notesDir, filename).delete()
    }

    /** Lists all entries, newest first (filenames sort chronologically). */
    suspend fun listAll(): List<EntryFile> = withContext(Dispatchers.IO) {
        notesDir.listFiles { f -> f.isFile && f.name.endsWith(".md") }
            ?.sortedByDescending { it.name }
            ?.mapNotNull { file ->
                val createdAt = parseTimestampFromFilename(file.name) ?: file.lastModified()
                runCatching {
                    val raw = file.readText()
                    val (date, time, body) = parseHeader(raw)
                    EntryFile(file.name, date, time, body, createdAt)
                }.getOrNull()
            }
            ?: emptyList()
    }

    private fun parseTimestampFromFilename(filename: String): Long? {
        val stem = filename.removeSuffix(".md")
        return runCatching { filenameFormat.parse(stem)?.time }.getOrNull()
    }

    /** Splits "# date\n### time\n\nbody" back into its three parts. Falls back gracefully if a file doesn't match the expected shape. */
    private fun parseHeader(raw: String): Triple<String, String, String> {
        val lines = raw.split("\n")
        val dateLine = lines.getOrNull(0)?.removePrefix("# ")?.trim() ?: ""
        val timeLine = lines.getOrNull(1)?.removePrefix("### ")?.trim() ?: ""
        val body = lines.drop(2).joinToString("\n").trimStart('\n')
        return Triple(dateLine, timeLine, body)
    }

    private fun stripHeader(raw: String): String = parseHeader(raw).third
}

