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
import androidx.compose.runtime.Composable
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
import com.example.notesapp_apv_czg.security.VaultState
import kotlinx.coroutines.flow.StateFlow
import com.example.notesapp_apv_czg.ui.components.AttachmentViewer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    viewModel: NoteViewModel,
    vaultState: StateFlow<VaultState>,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onRequestUnlock: (onResult: (Boolean) -> Unit) -> Unit
) {
    LaunchedEffect(noteId) {
        viewModel.getNoteById(noteId)
    }

    val currentNote by viewModel.currentNote.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentNote?.title ?: stringResource(R.string.note),
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
                        onClick = { currentNote?.let { onEdit(it.id) } },
                        enabled = currentNote != null
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        val haptic = LocalHapticFeedback.current
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

            val vault by vaultState.collectAsState()
            var isUnlocked by remember(note.id) { mutableStateOf(false) }

            LaunchedEffect(vault) {
                if (vault is VaultState.Locked) {
                    isUnlocked = false
                }
            }

            if (note.isLocked && !isUnlocked) {
                Text(
                    text = "🔐 Nota protegida",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        onRequestUnlock { success ->
                            if (success) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isUnlocked = true
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
                note.description?.takeIf { it.isNotEmpty() }?.let { description ->
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
