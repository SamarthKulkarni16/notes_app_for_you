package com.samarth.notesapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samarth.notesapp.data.local.EntryFile
import com.samarth.notesapp.data.local.EntryFileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the read-only history screen. Reads directly from the local .md
 * files — never from Supabase — so history works fully offline and never
 * shows a loading spinner waiting on a network call.
 */
class HistoryViewModel(private val fileStore: EntryFileStore) : ViewModel() {

    private val _entries = MutableStateFlow<List<EntryFile>>(emptyList())
    val entries: StateFlow<List<EntryFile>> = _entries.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads the entry list from disk. Call when entering this screen,
     *  since a new entry may have been written since the list was last loaded. */
    fun refresh() {
        viewModelScope.launch {
            _entries.value = fileStore.listAll()
        }
    }
}
