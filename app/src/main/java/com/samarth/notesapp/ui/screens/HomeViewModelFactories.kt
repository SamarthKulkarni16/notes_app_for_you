package com.samarth.notesapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samarth.notesapp.data.local.EntryFileStore
import com.samarth.notesapp.sync.SyncManager

class WriteViewModelFactory(
    private val fileStore: EntryFileStore,
    private val syncManager: SyncManager,
    private val currentUserId: () -> String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WriteViewModel::class.java)) {
            return WriteViewModel(fileStore, syncManager, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}

class HistoryViewModelFactory(
    private val fileStore: EntryFileStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(fileStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
