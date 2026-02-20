package com.example.notesapp_apv_czg

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.notesapp_apv_czg.broadcastreceivers.cancelTaskReminder
import com.example.notesapp_apv_czg.broadcastreceivers.scheduleTaskReminder
import com.example.notesapp_apv_czg.data.AppDatabase
import com.example.notesapp_apv_czg.data.AndroidStructuredLogger
import com.example.notesapp_apv_czg.data.Note
import com.example.notesapp_apv_czg.data.NoteRepository
import com.example.notesapp_apv_czg.security.AppLockManager
import com.example.notesapp_apv_czg.security.BiometricAuthenticator
import com.example.notesapp_apv_czg.security.NoteCrypto
import com.example.notesapp_apv_czg.security.SecureKeyManager
import com.example.notesapp_apv_czg.security.VaultState
import com.example.notesapp_apv_czg.ui.NoteEditorScreen
import com.example.notesapp_apv_czg.ui.NoteListScreen
import com.example.notesapp_apv_czg.ui.NoteViewModel
import com.example.notesapp_apv_czg.ui.NoteDetailScreen
import com.example.notesapp_apv_czg.ui.VaultLockScreen
import com.example.notesapp_apv_czg.ui.theme.NotesAppAPVCZGTheme
import com.example.notesapp_apv_czg.ui.theme.ThemeSettingsScreen
import com.example.notesapp_apv_czg.ui.theme.ThemeManager

class MainActivity : FragmentActivity() {
    private lateinit var appLockManager: AppLockManager
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Handle the permission results if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        requestPermissions()

        val db = AppDatabase.getInstance(applicationContext)
        val logger = AndroidStructuredLogger
        val secureKeyManager = SecureKeyManager()
        val noteCrypto = NoteCrypto(secureKeyManager)
        val repo = NoteRepository(
            dao = db.noteDao(),
            logger = logger,
            crypto = noteCrypto
        )
        appLockManager = AppLockManager()
        val biometricAuthenticator = BiometricAuthenticator(secureKeyManager)

        setContent {
            NotesAppAPVCZGTheme {
                val nav = rememberNavController()
                val vm: NoteViewModel = viewModel(factory = NoteViewModelFactory(repo))
                val scope = rememberCoroutineScope()
                val vaultState by appLockManager.vaultState.collectAsState()
                var lastAuthFailed by remember { mutableStateOf(false) }
                androidx.compose.runtime.LaunchedEffect(vaultState) {
                    if (vaultState is VaultState.Locked) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = nav,
                            startDestination = "list",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                        composable("list") {
                            NoteListScreen(
                                notes = vm.notes.collectAsState().value,
                                onAdd = {
                                    vm.clearCurrentNote()
                                    nav.navigate("edit/0") // Navigate with a new note ID
                                },
                                onOpen = { id: Long -> nav.navigate("detail/$id") },
                                onDelete = { note: Note ->
                                    vm.delete(note)
                                    cancelTaskReminder(this@MainActivity, note)
                                },
                                onToggleLock = { note: Note, locked: Boolean ->
                                    vm.update(note.copy(isLocked = locked))
                                },
                                onToggleComplete = { note: Note ->
                                    val updated = note.copy(isCompleted = !note.isCompleted)
                                    vm.update(updated)
                                    if (updated.isCompleted) {
                                        cancelTaskReminder(this@MainActivity, updated)
                                    } else if (updated.isTask && updated.dueDateMillis != null) {
                                        scheduleTaskReminder(this@MainActivity, updated)
                                    }
                                },
                                onToggleFavorite = { note: Note ->
                                    vm.toggleFavorite(note)
                                },
                                onOpenThemeSettings = { nav.navigate("settings/theme") }
                            )
                        }
                        composable("edit/{id}") { backStack ->
                            val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                            val noteId = if (id == 0L) null else id
                            val currentNote by vm.currentNote.collectAsState()
                            
                            NoteEditorScreen(
                                noteId = noteId,
                                viewModel = vm,
                                onCancel = { nav.popBackStack() },
                                onSave = { savedNote ->
                                    if (savedNote.isTask && !savedNote.isCompleted && savedNote.dueDateMillis != null) {
                                        scheduleTaskReminder(this@MainActivity, savedNote)
                                    } else {
                                        cancelTaskReminder(this@MainActivity, savedNote)
                                    }
                                    nav.popBackStack()
                                }
                            )
                        }
                        composable("detail/{id}") { backStack ->
                            val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                            NoteDetailScreen(
                                noteId = id,
                                viewModel = vm,
                                vaultState = appLockManager.vaultState,
                                    noteCrypto = noteCrypto,
                                onBack = { nav.popBackStack() },
                                onEdit = { noteId: Long -> nav.navigate("edit/$noteId") },
                                onRequestUnlock = { onResult ->
                                    scope.launch {
                                        lastAuthFailed = false
                                        val result = biometricAuthenticator.authenticateForDecryption(
                                            activity = this@MainActivity,
                                            title = getString(R.string.unlock_note_title),
                                            subtitle = getString(R.string.app_name)
                                        , iv = ByteArray(12)
                                        ) // IV real se obtiene del texto cifrado cuando se integre por nota
                                        when (result) {
                                            is BiometricAuthenticator.AuthResult.Success -> {
                                                appLockManager.onAuthenticated()
                                                onResult(true)
                                            }
                                            is BiometricAuthenticator.AuthResult.Failed -> {
                                                lastAuthFailed = true
                                                onResult(false)
                                            }
                                            is BiometricAuthenticator.AuthResult.Cancelled -> {
                                                onResult(false)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        composable("settings/theme") {
                            val scope = rememberCoroutineScope()
                            val context = LocalContext.current
                            ThemeSettingsScreen(
                                onNavigateUp = { nav.popBackStack() },
                                currentScheme = ThemeManager.getCurrentScheme(),
                                onSchemeSelected = { scheme ->
                                    scope.launch {
                                        ThemeManager.setColorScheme(context, scheme)
                                    }
                                }
                            )
                        }
                    }
                        VaultLockScreen(
                            visible = vaultState is VaultState.Locked,
                            onUnlockClick = {
                                scope.launch {
                                    lastAuthFailed = false
                                    val result = biometricAuthenticator.authenticateForEncryption(
                                        activity = this@MainActivity,
                                        title = getString(R.string.app_name),
                                        subtitle = getString(R.string.unlock_note_title)
                                    )
                                    when (result) {
                                        is BiometricAuthenticator.AuthResult.Success -> {
                                            appLockManager.onAuthenticated()
                                        }
                                        is BiometricAuthenticator.AuthResult.Failed -> {
                                            lastAuthFailed = true
                                        }
                                        is BiometricAuthenticator.AuthResult.LockedOut -> {
                                            lastAuthFailed = true
                                        }
                                        is BiometricAuthenticator.AuthResult.Cancelled -> {
                                        }
                                    }
                                }
                            },
                            lastAuthFailed = lastAuthFailed
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::appLockManager.isInitialized) {
            when (appLockManager.vaultState.value) {
                is VaultState.Locked -> {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                }
                is VaultState.Unlocked -> {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NotificationReceiver.CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

class NoteViewModelFactory(private val repo: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NotesAppAPVCZGTheme {
        NoteListScreen(
            notes = emptyList(),
            onAdd = {},
            onOpen = { _: Long -> },
            onDelete = { _: Note -> },
            onToggleFavorite = { _: Note -> }
        )
    }
}
