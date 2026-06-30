package com.samarth.notesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samarth.notesapp.auth.AuthRepository
import com.samarth.notesapp.auth.AuthViewModel
import com.samarth.notesapp.auth.AuthViewModelFactory
import com.samarth.notesapp.auth.GoogleSignInHelper
import com.samarth.notesapp.ui.screens.HistoryScreen
import com.samarth.notesapp.ui.screens.HistoryViewModel
import com.samarth.notesapp.ui.screens.HistoryViewModelFactory
import com.samarth.notesapp.ui.screens.SignInScreen
import com.samarth.notesapp.ui.screens.WriteScreen
import com.samarth.notesapp.ui.screens.WriteViewModel
import com.samarth.notesapp.ui.screens.WriteViewModelFactory
import com.samarth.notesapp.ui.theme.NotesAppTheme
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as NotesApp
        val supabase = app.supabase

        val authRepository = AuthRepository(supabase)
        val googleSignInHelper = GoogleSignInHelper(
            context = this,
            webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        )
        val authViewModelFactory = AuthViewModelFactory(authRepository, googleSignInHelper)
        val writeViewModelFactory = WriteViewModelFactory(
            fileStore = app.entryFileStore,
            syncManager = app.syncManager,
            currentUserId = { authRepository.currentUserId() }
        )
        val historyViewModelFactory = HistoryViewModelFactory(app.entryFileStore)

        setContent {
            NotesAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
                    NotesAppRoot(
                        authViewModel = authViewModel,
                        writeViewModelFactory = writeViewModelFactory,
                        historyViewModelFactory = historyViewModelFactory,
                        app = app,
                        currentUserId = { authRepository.currentUserId() }
                    )
                }
            }
        }
    }
}

private enum class Screen { WRITE, HISTORY }

/**
 * Top-level router: shows the sign-in screen while signed out, otherwise
 * toggles between Write and History on triple-tap. Also kicks off a sync
 * pass on launch and every time connectivity is regained, so anything
 * written while offline catches up to Supabase automatically.
 */
@Composable
private fun NotesAppRoot(
    authViewModel: AuthViewModel,
    writeViewModelFactory: WriteViewModelFactory,
    historyViewModelFactory: HistoryViewModelFactory,
    app: NotesApp,
    currentUserId: () -> String?
) {
    val sessionStatus by authViewModel.sessionStatus.collectAsState()

    when (sessionStatus) {
        is SessionStatus.Authenticated -> {
            var screen by remember { mutableStateOf(Screen.WRITE) }

            // viewModel() is scoped to this Activity, so the SAME
            // WriteViewModel instance persists across Write <-> History
            // toggles within one app session. That is fine for autosave
            // continuity, but "always a fresh blank entry" on re-entering
            // Write must be done explicitly via startFreshEntry() below —
            // it does NOT happen automatically just because the `when`
            // branch changes.
            val writeViewModel: WriteViewModel = viewModel(factory = writeViewModelFactory)
            val historyViewModel: HistoryViewModel = viewModel(factory = historyViewModelFactory)

            // Retry any unsynced entries on launch and whenever the device
            // comes back online — catches up anything written offline.
            LaunchedEffect(Unit) {
                val userId = currentUserId() ?: return@LaunchedEffect
                launch { app.syncManager.syncPendingEntries(userId) }
                launch {
                    app.connectivityObserver.observeOnlineEvents().collect {
                        app.syncManager.syncPendingEntries(userId)
                    }
                }
            }

            when (screen) {
                Screen.WRITE -> {
                    WriteScreen(
                        viewModel = writeViewModel,
                        onTripleTapToHistory = { screen = Screen.HISTORY }
                    )
                }
                Screen.HISTORY -> {
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onTripleTapToWrite = {
                            // Resume the current draft — startFreshEntry() is
                            // intentionally NOT called here. Fresh blank only
                            // happens on actual app open (new WriteViewModel
                            // instance = new filename), not on in-session
                            // write<->history toggles.
                            screen = Screen.WRITE
                        }
                    )
                }
            }
        }

        is SessionStatus.NotAuthenticated,
        is SessionStatus.RefreshFailure -> SignInScreen(authViewModel)

        is SessionStatus.Initializing -> Unit
    }
}
