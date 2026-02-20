package com.example.notesapp_apv_czg.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.animateItemPlacement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.rememberAsyncImagePainter
import com.example.notesapp_apv_czg.R
import com.example.notesapp_apv_czg.data.Note
import com.example.notesapp_apv_czg.ui.components.PinDialog
import com.example.notesapp_apv_czg.ui.theme.ColorTokens
import com.example.notesapp_apv_czg.ui.theme.ElevationTokens
import com.example.notesapp_apv_czg.ui.theme.ShapeTokens
import com.example.notesapp_apv_czg.security.PinManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: (Note) -> Unit,
    onDelete: () -> Unit,
    onToggleLock: (Note, Boolean) -> Unit,
    onToggleComplete: (Note) -> Unit,
    onToggleFavorite: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Mostrar diálogo de eliminación sin dejar el item en estado dismiss
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Toggle de favorito y recuperar a estado Settled
                    onToggleFavorite(note)
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val target = dismissState.targetValue
            val color by animateColorAsState(
                targetValue = when (target) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surface
                },
                label = "background_color"
            )
            val deleteScale by animateFloatAsState(
                targetValue = if (target == SwipeToDismissBoxValue.EndToStart) 1.3f else 0.8f,
                label = "delete_scale"
            )
            val starScale by animateFloatAsState(
                targetValue = if (target == SwipeToDismissBoxValue.StartToEnd) 1.3f else 0.8f,
                label = "star_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, ShapeTokens.card)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .scale(starScale),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .scale(deleteScale),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        modifier = modifier
    ) {
        NoteCardContent(
            note = note,
            onClick = onClick,
            onToggleLock = onToggleLock,
            onToggleComplete = onToggleComplete,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NoteCardContent(
    note: Note,
    onClick: (Note) -> Unit,
    onToggleLock: (Note, Boolean) -> Unit,
    onToggleComplete: (Note) -> Unit,
    onToggleFavorite: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val checkScale = remember { Animatable(1f) }
    val favoriteScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(note.isFavorite) {
        favoriteScale.snapTo(0.97f)
        favoriteScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
        )
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(
                scaleX = favoriteScale.value,
                scaleY = favoriteScale.value
            )
            .clickable { onClick(note) },
        elevation = CardDefaults.cardElevation(defaultElevation = ElevationTokens.card),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isTask && note.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = ShapeTokens.card
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (note.isTask) {
                        Icon(
                            imageVector = if (note.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (note.isCompleted) "Marcar como incompleta" else "Marcar como completa",
                            tint = if (note.isCompleted) ColorTokens.taskCompleted else MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(checkScale.value)
                                .clickable {
                                    scope.launch {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleComplete(note)
                                        checkScale.snapTo(1f)
                                        checkScale.animateTo(
                                            1.05f,
                                            animationSpec = tween(
                                                durationMillis = 180,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        checkScale.animateTo(
                                            1f,
                                            animationSpec = tween(
                                                durationMillis = 180,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                }
                        )
                        Text(
                            text = stringResource(R.string.task),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (note.isTask && note.priority > 0) {
                    Row {
                        repeat(note.priority) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = ColorTokens.favorite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onToggleFavorite(note) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (note.isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                            tint = if (note.isFavorite) ColorTokens.favorite else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { onToggleLock(note, !note.isLocked) }) {
                        Icon(
                            imageVector = if (note.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (note.isLocked) stringResource(R.string.locked_note) else note.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (note.isTask && note.isCompleted) TextDecoration.LineThrough else null
                ),
                color = when {
                    note.isLocked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    note.isTask && note.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!note.isLocked) {
                note.description?.takeIf { it.isNotEmpty() }?.let { description ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (note.attachmentUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AttachmentPreview(attachmentUris = note.attachmentUris)
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.locked_note_preview_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    note.dueDateMillis?.let { dueDate ->
                        val isOverdue = dueDate < System.currentTimeMillis() && !note.isCompleted
                        Text(
                            text = stringResource(R.string.due_date, SimpleDateFormat.getDateInstance().format(Date(dueDate))),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            fontWeight = if (isOverdue) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                if (!note.isLocked && note.attachmentUris.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = note.attachmentUris.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    notes: List<Note>,
    onAdd: () -> Unit = {},
    onOpen: (Long) -> Unit = {},
    onDelete: (Note) -> Unit = {},
    onToggleLock: (Note, Boolean) -> Unit = { _, _ -> },
    onToggleComplete: (Note) -> Unit = {},
    onToggleFavorite: (Note) -> Unit = {},
    onOpenThemeSettings: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("all") }
    var showDeleteDialog by remember { mutableStateOf<Note?>(null) }
    var pinTargetNote by remember { mutableStateOf<Note?>(null) }
    var pinUnlockTarget by remember { mutableStateOf<Note?>(null) }
    var pinSetTarget by remember { mutableStateOf<Note?>(null) }
    val context = LocalContext.current

    val filteredNotes by remember(notes, searchQuery, filterType) {
        derivedStateOf {
            notes.filter { note ->
                val matchesSearch = note.title.contains(searchQuery, ignoreCase = true) ||
                        note.description?.contains(searchQuery, ignoreCase = true) == true
                val matchesFilter = when (filterType) {
                    "notes" -> !note.isTask
                    "tasks" -> note.isTask
                    "favorites" -> note.isFavorite
                    else -> true
                }
                matchesSearch && matchesFilter
            }.sortedWith(
                compareByDescending<Note> { it.isFavorite }
                    .thenByDescending { it.isTask && !it.isCompleted }
                    .thenByDescending { it.priority }
                    .thenByDescending { it.dueDateMillis ?: 0 }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onOpenThemeSettings) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = stringResource(R.string.theme),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_note_task))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.new_note_task),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_notes)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filterType == "all",
                            onClick = { filterType = "all" },
                            label = { Text(stringResource(R.string.all), fontWeight = FontWeight.Medium) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == "notes",
                            onClick = { filterType = "notes" },
                            label = { Text(stringResource(R.string.notes), fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == "tasks",
                            onClick = { filterType = "tasks" },
                            label = { Text(stringResource(R.string.tasks), fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Task,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == "favorites",
                            onClick = { filterType = "favorites" },
                            label = { Text(stringResource(R.string.favorites), fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(com.example.notesapp_apv_czg.ui.theme.Spacing.m))

                if (filteredNotes.isEmpty()) {
                    EmptyState(hasSearch = searchQuery.isNotEmpty())
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isLargeScreen = maxWidth >= 600.dp
                        if (isLargeScreen) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 320.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredNotes, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        onClick = {
                                            if (note.isLocked) {
                                                pinTargetNote = note
                                            } else {
                                                onOpen(note.id)
                                            }
                                        },
                                        onDelete = { showDeleteDialog = note },
                                        onToggleLock = { n, desiredLocked ->
                                            if (!desiredLocked) {
                                                pinUnlockTarget = n
                                            } else {
                                                if (!PinManager.isPinSet(context)) {
                                                    pinSetTarget = n
                                                } else {
                                                    onToggleLock(n, true)
                                                }
                                            }
                                        },
                                        onToggleComplete = onToggleComplete,
                                        onToggleFavorite = onToggleFavorite,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItemPlacement()
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = filteredNotes,
                                    key = { it.id }
                                ) { note ->
                                    NoteCard(
                                        note = note,
                                        onClick = {
                                            if (note.isLocked) {
                                                pinTargetNote = note
                                            } else {
                                                onOpen(note.id)
                                            }
                                        },
                                        onDelete = { showDeleteDialog = note },
                                        onToggleLock = { n, desiredLocked ->
                                            if (!desiredLocked) {
                                                pinUnlockTarget = n
                                            } else {
                                                if (!PinManager.isPinSet(context)) {
                                                    pinSetTarget = n
                                                } else {
                                                    onToggleLock(n, true)
                                                }
                                            }
                                        },
                                        onToggleComplete = onToggleComplete,
                                        onToggleFavorite = onToggleFavorite,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItemPlacement()
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
            }
        }

        showDeleteDialog?.let { note ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text(stringResource(R.string.delete_note_title)) },
                text = { Text(stringResource(R.string.delete_note_confirmation)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete(note)
                            showDeleteDialog = null
                        }
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        pinTargetNote?.let { target ->
            PinDialog(
                onSuccess = {
                    onOpen(target.id)
                    pinTargetNote = null
                },
                onCancel = { pinTargetNote = null },
                title = stringResource(R.string.unlock_note_title)
            )
        }

        pinUnlockTarget?.let { target ->
            PinDialog(
                onSuccess = {
                    onToggleLock(target, false)
                    pinUnlockTarget = null
                },
                onCancel = { pinUnlockTarget = null },
                title = stringResource(R.string.unlock_note_title)
            )
        }

        pinSetTarget?.let { target ->
            PinDialog(
                onSuccess = {
                    onToggleLock(target, true)
                    pinSetTarget = null
                },
                onCancel = { pinSetTarget = null },
                title = stringResource(R.string.configure_pin_title)
            )
        }
    }
}

@Composable
fun EmptyState(
    hasSearch: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasSearch) Icons.Default.Search else Icons.AutoMirrored.Filled.Notes,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (hasSearch) 
                stringResource(R.string.no_notes_found) 
            else 
                stringResource(R.string.empty_list_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
