package com.samarth.notesapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samarth.notesapp.data.local.EntryFile
import com.samarth.notesapp.ui.components.TripleTapBox
import com.samarth.notesapp.ui.theme.InputBorderColor
import com.samarth.notesapp.ui.theme.MutedColor

/**
 * Read-only, newest-first list of past entries. Triple-tapping anywhere
 * empty (including the empty-state message) returns to the write screen.
 * Reads from local .md files only, so this works fully offline.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onTripleTapToWrite: () -> Unit
) {
    val entries by viewModel.entries.collectAsState()

    TripleTapBox(onTripleTap = onTripleTapToWrite) {
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nothing written yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedColor
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(entries, key = { it.filename }) { entry ->
                    HistoryEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryRow(entry: EntryFile) {
    Column {
        Text(
            text = "${entry.dateLabel} · ${entry.timeLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MutedColor
        )
        Text(
            text = entry.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp)
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = InputBorderColor
        )
    }
}
