package com.samarth.notesapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samarth.notesapp.data.local.EntryFileStore
import com.samarth.notesapp.sync.SyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val AUTOSAVE_DEBOUNCE_MILLIS = 600L

/** Mirrors the status states the web Notes editor already shows in its corner dot. */
enum class SaveStatus {
    IDLE,           // nothing typed yet this session
    SAVING,         // debounce timer running, about to write to disk
    SAVED_LOCALLY,  // .md file written, sync not attempted/finished yet
    SYNCING,        // actively pushing to Supabase
    SYNCED,         // confirmed on Supabase
    SYNC_FAILED     // local save is safe; cloud push failed, will retry later
}

class WriteViewModel(
    private val fileStore: EntryFileStore,
    private val syncManager: SyncManager,
    private val currentUserId: () -> String?
) : ViewModel() {

    // A fresh filename — and fresh date/time labels — are chosen once, the
    // moment this ViewModel is created (i.e. once per app open), matching
    // "always a fresh blank entry." These labels are captured here rather
    // than recomputed on every autosave so the header doesn't drift to a
    // later time if someone writes for several minutes.
    private var currentFilename: String = fileStore.newFilenameForNow()

    val dateLabel: String = fileStore.currentDateLabel()
    val timeLabel: String = fileStore.currentTimeLabel()

    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()

    private val _saveStatus = MutableStateFlow(SaveStatus.IDLE)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private var saveJob: Job? = null

    fun onBodyChanged(newBody: String) {
        _body.value = newBody
        if (newBody.isNotBlank()) {
            _saveStatus.value = SaveStatus.SAVING
        }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MILLIS)
            persistAndSync(newBody)
        }
    }

    private suspend fun persistAndSync(text: String) {
        if (text.isBlank()) {
            _saveStatus.value = SaveStatus.IDLE
            return
        }

        fileStore.write(currentFilename, dateLabel, timeLabel, text)
        _saveStatus.value = SaveStatus.SAVED_LOCALLY

        val userId = currentUserId()
        if (userId == null) {
            // Signed-out edge case shouldn't normally be reachable from this
            // screen, but if it happens, the .md file is still safe on disk
            // and will sync next time a session exists.
            return
        }

        _saveStatus.value = SaveStatus.SYNCING
        val synced = syncManager.registerAndSync(currentFilename, userId)
        _saveStatus.value = if (synced) SaveStatus.SYNCED else SaveStatus.SYNC_FAILED
    }
}
