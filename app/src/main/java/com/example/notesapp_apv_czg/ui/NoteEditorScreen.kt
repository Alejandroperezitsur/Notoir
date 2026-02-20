package com.example.notesapp_apv_czg.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.notesapp_apv_czg.R
import com.example.notesapp_apv_czg.data.Note
import com.example.notesapp_apv_czg.ui.components.AttachmentOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import com.example.notesapp_apv_czg.ui.theme.ColorTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long?,
    viewModel: NoteViewModel,
    onCancel: () -> Unit,
    onSave: (Note) -> Unit
) {
    val isNewNote = noteId == null
    val currentNote by viewModel.currentNote.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(noteId) {
        if (noteId != null) {
            viewModel.getNoteById(noteId)
        } else {
            viewModel.clearCurrentNote()
        }
    }

    var editorState by remember {
        mutableStateOf(
            EditorUiState(
                mode = EditorMode.NoteMode,
                title = TextFieldValue(""),
                body = TextFieldValue(""),
                isLocked = false,
                isCompleted = false,
                priority = 0,
                hasReminder = false,
                reminderDate = null,
                attachments = emptyList(),
                tags = emptyList(),
                isFavorite = false,
                isSaving = false
            )
        )
    }
    var newTag by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    val audioRecorder = remember { AudioRecorder(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    var recordingSeconds by remember { mutableStateOf(0) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    val originalNote = remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds += 1
            }
        }
    }

    LaunchedEffect(currentNote) {
        currentNote?.let { note ->
            editorState = EditorUiState(
                mode = if (note.isTask) EditorMode.TaskMode else EditorMode.NoteMode,
                title = TextFieldValue(note.title),
                body = TextFieldValue(note.description ?: ""),
                isLocked = note.isLocked,
                isCompleted = note.isCompleted,
                priority = note.priority,
                hasReminder = note.dueDateMillis != null,
                reminderDate = note.dueDateMillis,
                attachments = note.attachmentUris.map { AttachmentUiModel(it) },
                tags = note.tags,
                isFavorite = note.isFavorite,
                isSaving = isSaving
            )
            originalNote.value = note
        } ?: run {
            if (isNewNote) {
                originalNote.value = Note(
                    id = 0,
                    title = "",
                    description = "",
                    isTask = false,
                    isCompleted = false,
                    priority = 0,
                    dueDateMillis = null,
                    attachmentUris = emptyList(),
                    isLocked = false,
                    tags = emptyList(),
                    isFavorite = false
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioPlaybackCoordinator.pauseAll()
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearErrorMessage()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            editorState = editorState.copy(
                attachments = editorState.attachments + AttachmentUiModel(it.toString())
            )
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            editorState = editorState.copy(
                attachments = editorState.attachments + AttachmentUiModel(it.toString())
            )
        }
    }

    fun buildDraftNote(existingId: Long?): Note {
        val attachmentUris = editorState.attachments.map { it.uri }
        return Note(
            id = existingId ?: 0,
            title = editorState.title.text,
            description = editorState.body.text,
            isTask = editorState.mode is EditorMode.TaskMode,
            isCompleted = editorState.isCompleted,
            priority = editorState.priority,
            dueDateMillis = editorState.reminderDate,
            attachmentUris = attachmentUris,
            isLocked = editorState.isLocked,
            tags = editorState.tags,
            isFavorite = editorState.isFavorite
        )
    }

    val isDirty by remember(editorState, originalNote.value, isNewNote) {
        androidx.compose.runtime.derivedStateOf {
            originalNote.value?.let { original ->
                val draft = buildDraftNote(if (isNewNote) 0 else original.id)
                draft != original
            } ?: (
                editorState.title.text.isNotEmpty() ||
                    editorState.body.text.isNotEmpty() ||
                    editorState.mode is EditorMode.TaskMode ||
                    editorState.isCompleted ||
                    editorState.priority != 0 ||
                    editorState.reminderDate != null ||
                    editorState.attachments.isNotEmpty() ||
                    editorState.isLocked ||
                    editorState.tags.isNotEmpty() ||
                    editorState.isFavorite
                )
        }
    }

    fun saveNoteInternal() {
        val baseId = currentNote?.id
        val note = buildDraftNote(baseId)
        if (isNewNote) {
            viewModel.insert(note) { id ->
                onSave(note.copy(id = id))
            }
        } else {
            viewModel.update(note)
            onSave(note)
        }
    }

    fun createImageUri(): Uri {
        val imageFile = File(context.cacheDir, "camera_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
        editorState = editorState.copy(
            attachments = editorState.attachments + AttachmentUiModel(uri.toString())
        )
        return uri
    }

    fun onEditorStateChange(newState: EditorUiState) {
        editorState = newState
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { hostState ->
            SnackbarHost(hostState) { data ->
                androidx.compose.material3.Snackbar(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = data.visuals.message)
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNewNote) stringResource(R.string.new_note_task) else stringResource(
                            R.string.save
                        ),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isDirty && !isSaving) {
                                showExitConfirmation = true
                            } else {
                                onCancel()
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                actions = {
                    SecuritySection(
                        isLocked = editorState.isLocked,
                        onLockedChange = { locked ->
                            editorState = editorState.copy(isLocked = locked)
                        },
                        isNewNote = isNewNote,
                        currentNote = currentNote,
                        title = editorState.title.text,
                        description = editorState.body,
                        mode = editorState.mode,
                        isCompleted = editorState.isCompleted,
                        priority = editorState.priority,
                        dueDateMillis = editorState.reminderDate,
                        attachmentUris = editorState.attachments.map { it.uri },
                        viewModel = viewModel
                    )
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        sheetPeekHeight = 0.dp,
        sheetContent = {
            AttachmentOptions(
                onCameraClick = {
                    scope.launch {
                        scaffoldState.bottomSheetState.partialExpand()
                        val uri = createImageUri()
                        cameraLauncher.launch(uri)
                    }
                },
                onGalleryClick = {
                    scope.launch {
                        scaffoldState.bottomSheetState.partialExpand()
                        galleryLauncher.launch("image/*")
                    }
                },
                onAudioClick = {
                    scope.launch {
                        scaffoldState.bottomSheetState.partialExpand()
                        audioLauncher.launch("audio/*")
                    }
                },
                onRecordClick = {
                    handleRecordClick(
                        context = context,
                        isRecording = isRecording,
                        audioRecorder = audioRecorder,
                        audioFile = audioFile,
                        onIsRecordingChange = { isRecording = it },
                        onAudioFileChange = { audioFile = it },
                        onAttachmentAdded = { uriString ->
                            editorState = editorState.copy(
                                attachments = editorState.attachments + AttachmentUiModel(uriString)
                            )
                        },
                        scaffoldState = scaffoldState,
                        snackbarHostState = snackbarHostState,
                        scope = scope
                    )
                },
                isRecording = isRecording
            )
        },
        sheetDragHandle = null,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.imePadding()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (editorState.mode is EditorMode.TaskMode) {
                TaskEditorContent(
                    editorState = editorState,
                    onEditorStateChange = ::onEditorStateChange,
                    newTag = newTag,
                    onNewTagChange = { newTag = it }
                )
            } else {
                NoteEditorContent(
                    editorState = editorState,
                    onEditorStateChange = ::onEditorStateChange,
                    newTag = newTag,
                    onNewTagChange = { newTag = it }
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val sheetState = scaffoldState.bottomSheetState
                            if (sheetState.currentValue == androidx.compose.material3.SheetValue.Expanded) {
                                sheetState.partialExpand()
                            } else {
                                sheetState.expand()
                            }
                        }
                    },
                    containerColor = ColorTokens.accent,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = stringResource(R.string.attachments),
                        modifier = Modifier.size(24.dp)
                    )
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        if (!isSaving) {
                            saveNoteInternal()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    containerColor = ColorTokens.success,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    enabled = !isSaving
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = stringResource(R.string.save)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.save),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (showExitConfirmation) {
                AlertDialog(
                    onDismissRequest = { showExitConfirmation = false },
                    title = { Text("Cambios sin guardar") },
                    text = { Text("Si sales ahora perderás los cambios no guardados.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showExitConfirmation = false
                                onCancel()
                            }
                        ) {
                            Text("Descartar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitConfirmation = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NoteEditorContent(
    editorState: EditorUiState,
    onEditorStateChange: (EditorUiState) -> Unit,
    newTag: String,
    onNewTagChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        EditorTextSection(
            title = editorState.title,
            onTitleChange = { value ->
                onEditorStateChange(editorState.copy(title = value))
            },
            description = editorState.body,
            onDescriptionChange = { value ->
                onEditorStateChange(editorState.copy(body = value))
            },
            isLocked = editorState.isLocked,
            onBold = {
                if (editorState.isLocked) return@EditorTextSection
                val description = editorState.body
                val selection = description.selection
                if (!selection.collapsed) {
                    val builder = AnnotatedString.Builder(description.annotatedString)
                    builder.addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        selection.min,
                        selection.max
                    )
                    onEditorStateChange(
                        editorState.copy(body = description.copy(annotatedString = builder.toAnnotatedString()))
                    )
                }
            },
            onItalic = {
                if (editorState.isLocked) return@EditorTextSection
                val description = editorState.body
                val selection = description.selection
                if (!selection.collapsed) {
                    val builder = AnnotatedString.Builder(description.annotatedString)
                    builder.addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        selection.min,
                        selection.max
                    )
                    onEditorStateChange(
                        editorState.copy(body = description.copy(annotatedString = builder.toAnnotatedString()))
                    )
                }
            },
            onChecklist = {
                if (editorState.isLocked) return@EditorTextSection
                val description = editorState.body
                val selection = description.selection
                val lineStart = description.text.lastIndexOf('\n', selection.start - 1)
                    .let { if (it < 0) 0 else it + 1 }
                val newText = description.text.substring(0, lineStart) +
                        "☐ " + description.text.substring(lineStart)
                onEditorStateChange(
                    editorState.copy(
                        body = TextFieldValue(
                            text = newText,
                            selection = TextRange(selection.start + 2)
                        )
                    )
                )
            },
            onBullet = {
                if (editorState.isLocked) return@EditorTextSection
                val description = editorState.body
                val selection = description.selection
                val lineStart = description.text.lastIndexOf('\n', selection.start - 1)
                    .let { if (it < 0) 0 else it + 1 }
                val newText = description.text.substring(0, lineStart) +
                        "• " + description.text.substring(lineStart)
                onEditorStateChange(
                    editorState.copy(
                        body = TextFieldValue(
                            text = newText,
                            selection = TextRange(selection.start + 2)
                        )
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TagSection(
            tags = editorState.tags,
            newTag = newTag,
            onNewTagChange = onNewTagChange,
            onAddTag = {
                val value = newTag.trim()
                if (value.isNotEmpty() && !editorState.tags.contains(value)) {
                    onEditorStateChange(editorState.copy(tags = editorState.tags + value))
                    onNewTagChange("")
                }
            },
            onRemoveTag = { tag ->
                onEditorStateChange(editorState.copy(tags = editorState.tags.filter { it != tag }))
            },
            enabled = !editorState.isLocked
        )

        Spacer(modifier = Modifier.height(16.dp))

        FavoriteSection(
            isFavorite = editorState.isFavorite,
            onToggle = { favorite ->
                onEditorStateChange(editorState.copy(isFavorite = favorite))
            },
            enabled = !editorState.isLocked
        )

        val attachmentUris = editorState.attachments.map { it.uri }
        if (attachmentUris.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            AttachmentsSection(
                attachmentUris = attachmentUris,
                onRemoveAttachment = { uri ->
                    onEditorStateChange(
                        editorState.copy(
                            attachments = editorState.attachments.filterNot { it.uri == uri }
                        )
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun TaskEditorContent(
    editorState: EditorUiState,
    onEditorStateChange: (EditorUiState) -> Unit,
    newTag: String,
    onNewTagChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        TaskOptionsSection(
            isTask = editorState.mode is EditorMode.TaskMode,
            onIsTaskChange = { isTask ->
                onEditorStateChange(
                    editorState.copy(
                        mode = if (isTask) EditorMode.TaskMode else EditorMode.NoteMode
                    )
                )
            },
            isCompleted = editorState.isCompleted,
            onIsCompletedChange = { completed ->
                onEditorStateChange(editorState.copy(isCompleted = completed))
            },
            priority = editorState.priority,
            onPriorityChange = { priority ->
                onEditorStateChange(editorState.copy(priority = priority))
            },
            dueDateMillis = editorState.reminderDate,
            onDueDateChange = { newDate ->
                onEditorStateChange(
                    editorState.copy(
                        hasReminder = newDate != null,
                        reminderDate = newDate
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        NoteEditorContent(
            editorState = editorState,
            onEditorStateChange = onEditorStateChange,
            newTag = newTag,
            onNewTagChange = onNewTagChange
        )
    }
}
