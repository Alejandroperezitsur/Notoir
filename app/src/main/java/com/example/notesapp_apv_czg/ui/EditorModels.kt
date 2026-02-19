package com.example.notesapp_apv_czg.ui

import androidx.compose.ui.text.input.TextFieldValue

sealed class EditorMode {
    object NoteMode : EditorMode()
    object TaskMode : EditorMode()
}

data class AttachmentUiModel(
    val uri: String
)

data class EditorUiState(
    val mode: EditorMode,
    val title: TextFieldValue,
    val body: TextFieldValue,
    val isLocked: Boolean,
    val isCompleted: Boolean,
    val priority: Int,
    val hasReminder: Boolean,
    val reminderDate: Long?,
    val attachments: List<AttachmentUiModel>,
    val tags: List<String>,
    val isFavorite: Boolean,
    val isSaving: Boolean
)


