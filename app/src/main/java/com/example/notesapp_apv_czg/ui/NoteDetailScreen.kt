package com.example.notesapp_apv_czg.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.notesapp_apv_czg.R
import com.example.notesapp_apv_czg.security.NoteCrypto
import com.example.notesapp_apv_czg.security.VaultState
import kotlinx.coroutines.flow.StateFlow
import com.example.notesapp_apv_czg.ui.components.AttachmentViewer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    viewModel: NoteViewModel,
    vaultState: StateFlow<VaultState>,
    noteCrypto: NoteCrypto,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onRequestUnlock: (onResult: (Boolean) -> Unit) -> Unit
) {
    LaunchedEffect(noteId) {
        viewModel.getNoteById(noteId)
    }

    val currentNote by viewModel.currentNote.collectAsState()
    val vault by vaultState.collectAsState()
    val haptic = LocalHapticFeedback.current
    var decryptedTitle by remember(currentNote?.id) { mutableStateOf<String?>(null) }
    var decryptedBody by remember(currentNote?.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(vault) {
        if (vault is VaultState.Locked) {
            decryptedTitle = null
            decryptedBody = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            decryptedTitle = null
            decryptedBody = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val note = currentNote
                    val titleText = when {
                        note == null -> stringResource(R.string.note)
                        note.isLocked && decryptedTitle != null -> decryptedTitle!!
                        note.isLocked -> stringResource(R.string.note)
                        else -> note.title
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val note = currentNote
                            if (note != null) {
                                if (!note.isLocked || vault is VaultState.Unlocked) {
                                    onEdit(note.id)
                                    val hapticLocal = LocalHapticFeedback.current
                                    hapticLocal.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } else {
                                    onRequestUnlock { success ->
                                        if (success) {
                                            onEdit(note.id)
                                            val hapticLocal = LocalHapticFeedback.current
                                            hapticLocal.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                            }
                        },
                        enabled = currentNote != null
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            val note = currentNote
            if (note == null) {
                Text(text = stringResource(R.string.search_notes))
                return@Column
            }
            if (note.isLocked && vault is VaultState.Locked) {
                Text(
                    text = stringResource(R.string.locked_note),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        onRequestUnlock { success ->
                            if (success) {
                                try {
                                    val decTitle = noteCrypto.decrypt(note.title)
                                    val decBody = note.description?.let { d -> noteCrypto.decrypt(d) }
                                    decryptedTitle = decTitle
                                    decryptedBody = decBody
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = "Desbloquear",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                // Description/content
                val bodyText = when {
                    note.isLocked && decryptedBody != null -> decryptedBody
                    !note.isLocked -> note.description
                    else -> null
                }
                bodyText?.takeIf { it.isNotEmpty() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Task due date
                if (note.isTask && note.dueDateMillis != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val formatted = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(note.dueDateMillis))
                    Text(
                        text = stringResource(R.string.due_date, formatted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Attachments viewer
                if (note.attachmentUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AttachmentViewer(
                        attachmentUris = note.attachmentUris,
                        onRemoveAttachment = { /* no-op in detail screen */ },
                        modifier = Modifier.fillMaxWidth(),
                        allowRemove = false
                    )
                }
            }
        }
    }
}
