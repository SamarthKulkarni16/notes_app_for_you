package com.samarth.notesapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.samarth.notesapp.ui.components.SaveStatusIndicator
import com.samarth.notesapp.ui.components.TripleTapBox
import com.samarth.notesapp.ui.theme.EntryHeaderStyle
import com.samarth.notesapp.ui.theme.MutedColor

/**
 * The home screen — laid out exactly like the web Notes editor:
 *
 *   [serif date]                     [status dot + label]
 *   [muted time]
 *
 *   Start writing… (placeholder, disappears once typing begins)
 *
 * Typing autosaves (see WriteViewModel). Triple-tapping anywhere empty
 * switches to history — text-field taps are unaffected since
 * BasicTextField consumes its own taps for cursor placement. The keyboard
 * opens automatically the moment this screen appears.
 */
@Composable
fun WriteScreen(
    viewModel: WriteViewModel,
    onTripleTapToHistory: () -> Unit
) {
    val body by viewModel.body.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    TripleTapBox(onTripleTap = onTripleTapToHistory) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Header row: date/time on the left, sync status on the right —
            // same flex space-between layout as the web editor's .header.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(text = viewModel.dateLabel, style = EntryHeaderStyle.Date)
                    Text(
                        text = viewModel.timeLabel.uppercase(),
                        style = EntryHeaderStyle.Time,
                        color = MutedColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                SaveStatusIndicator(status = saveStatus)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 36.dp)
            ) {
                if (body.isEmpty()) {
                    Text(
                        text = "Start writing…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = EntryHeaderStyle.PlaceholderColor
                    )
                }
                BasicTextField(
                    value = body,
                    onValueChange = viewModel::onBodyChanged,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
