package com.example.notesapp_apv_czg.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.notesapp_apv_czg.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

object AudioPlaybackCoordinator {
    private val players = mutableSetOf<MediaPlayer>()

    fun play(player: MediaPlayer) {
        players.add(player)
        try {
            player.start()
        } catch (_: Exception) {
        }
    }

    fun pause(player: MediaPlayer) {
        try {
            player.pause()
        } catch (_: Exception) {
        }
        players.add(player)
    }

    fun pauseAll() {
        players.forEach { p ->
            try {
                p.pause()
            } catch (_: Exception) {
            }
        }
        players.clear()
    }
}

@Composable
fun AudioAttachmentPlayer(uri: String) {
    val context = LocalContext.current
    var isPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableStateOf(0) }
    var positionMs by remember { mutableStateOf(0) }

    val mediaPlayer = remember(uri) { MediaPlayer() }

    DisposableEffect(uri) {
        try {
            val contentUri = android.net.Uri.parse(uri)
            val pfd = context.contentResolver.openFileDescriptor(contentUri, "r")
            mediaPlayer.reset()
            if (pfd != null) {
                mediaPlayer.setDataSource(pfd.fileDescriptor)
                pfd.close()
            } else {
                try {
                    mediaPlayer.setDataSource(context, contentUri)
                } catch (_: Exception) {
                    mediaPlayer.setDataSource(uri)
                }
            }
            mediaPlayer.prepare()
            isPrepared = true
            durationMs = mediaPlayer.duration
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                positionMs = 0
            }
        } catch (_: Exception) {
            isPrepared = false
        }
        onDispose {
            try {
                mediaPlayer.release()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(isPlaying, isPrepared) {
        if (isPrepared) {
            if (isPlaying) {
                try {
                    AudioPlaybackCoordinator.play(mediaPlayer)
                } catch (_: Exception) {
                }
            } else {
                try {
                    AudioPlaybackCoordinator.pause(mediaPlayer)
                } catch (_: Exception) {
                }
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            try {
                positionMs = mediaPlayer.currentPosition
            } catch (_: Exception) {
            }
            delay(250)
        }
    }

    val progress = remember(positionMs, durationMs) {
        if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = { if (isPrepared) isPlaying = !isPlaying },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Slider(
            value = progress,
            onValueChange = { value ->
                if (durationMs > 0) {
                    positionMs = (durationMs * value).toInt()
                    try {
                        mediaPlayer.seekTo(positionMs)
                    } catch (_: Exception) {
                    }
                }
            },
            modifier = Modifier.width(64.dp),
            valueRange = 0f..1f
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${formatMs(positionMs)} / ${formatMs(durationMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
fun handleRecordClick(
    context: Context,
    isRecording: Boolean,
    audioRecorder: AudioRecorder,
    audioFile: File?,
    onIsRecordingChange: (Boolean) -> Unit,
    onAudioFileChange: (File?) -> Unit,
    attachmentUris: SnapshotStateList<String>,
    scaffoldState: BottomSheetScaffoldState,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    if (isRecording) {
        audioRecorder.stop()
        onIsRecordingChange(false)
        audioFile?.let { file ->
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            attachmentUris.add(uri.toString())
        }
        onAudioFileChange(null)
        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
    } else {
        try {
            File(context.cacheDir, "audio_${java.util.UUID.randomUUID()}.3gp").also {
                audioRecorder.start(it)
                onAudioFileChange(it)
                onIsRecordingChange(true)
            }
        } catch (_: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.recording_start_failed))
            }
        }
    }
}
