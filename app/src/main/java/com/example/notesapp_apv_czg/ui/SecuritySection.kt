package com.example.notesapp_apv_czg.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.example.notesapp_apv_czg.R
import com.example.notesapp_apv_czg.data.Note
import com.example.notesapp_apv_czg.security.PinManager
import com.example.notesapp_apv_czg.ui.components.PinDialog

@Composable
fun SecuritySection(
    isLocked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    isNewNote: Boolean,
    currentNote: Note?,
    title: String,
    description: TextFieldValue,
    isTask: Boolean,
    isCompleted: Boolean,
    priority: Int,
    dueDateMillis: Long?,
    attachmentUris: List<String>,
    viewModel: NoteViewModel
) {
    val context = LocalContext.current
    var showPinSetDialog by remember { mutableStateOf(false) }
    var showPinUnlockDialog by remember { mutableStateOf(false) }

    if (showPinSetDialog) {
        PinDialog(
            onSuccess = {
                showPinSetDialog = false
                onLockedChange(true)
                if (!isNewNote) {
                    val updated = Note(
                        id = currentNote?.id ?: 0,
                        title = title,
                        description = description.text,
                        isTask = isTask,
                        isCompleted = isCompleted,
                        priority = priority,
                        dueDateMillis = dueDateMillis,
                        attachmentUris = attachmentUris.toList(),
                        isLocked = true
                    )
                    viewModel.update(updated)
                }
            },
            onCancel = { showPinSetDialog = false },
            title = stringResource(R.string.configure_pin_title)
        )
    }

    if (showPinUnlockDialog) {
        PinDialog(
            onSuccess = {
                showPinUnlockDialog = false
                onLockedChange(false)
                if (!isNewNote) {
                    val updated = Note(
                        id = currentNote?.id ?: 0,
                        title = title,
                        description = description.text,
                        isTask = isTask,
                        isCompleted = isCompleted,
                        priority = priority,
                        dueDateMillis = dueDateMillis,
                        attachmentUris = attachmentUris.toList(),
                        isLocked = false
                    )
                    viewModel.update(updated)
                }
            },
            onCancel = { showPinUnlockDialog = false },
            title = stringResource(R.string.unlock_note_title)
        )
    }

    IconButton(onClick = {
        if (isLocked) {
            showPinUnlockDialog = true
        } else {
            if (!PinManager.isPinSet(context)) {
                showPinSetDialog = true
            } else {
                onLockedChange(true)
                if (!isNewNote) {
                    val updated = Note(
                        id = currentNote?.id ?: 0,
                        title = title,
                        description = description.text,
                        isTask = isTask,
                        isCompleted = isCompleted,
                        priority = priority,
                        dueDateMillis = dueDateMillis,
                        attachmentUris = attachmentUris.toList(),
                        isLocked = true
                    )
                    viewModel.update(updated)
                }
            }
        }
    }) {
        Icon(
            imageVector = if (isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
            contentDescription = if (isLocked) "Desbloquear" else "Bloquear"
        )
    }
}

