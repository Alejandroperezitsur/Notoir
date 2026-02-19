package com.example.notesapp_apv_czg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.notesapp_apv_czg.ui.theme.ColorTokens
import com.example.notesapp_apv_czg.ui.theme.ElevationTokens
import com.example.notesapp_apv_czg.ui.theme.ShapeTokens
import com.example.notesapp_apv_czg.R

@Composable
fun AttachmentsSection(
    attachmentUris: List<String>,
    onRemoveAttachment: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.attachments),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 600.dp
                if (isWide) {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(attachmentUris.size) { index ->
                            val uri = attachmentUris[index]
                            AttachmentPreviewItem(
                                uri = uri,
                                onRemove = { onRemoveAttachment(uri) }
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(attachmentUris) { uri ->
                            AttachmentPreviewItem(
                                uri = uri,
                                onRemove = { onRemoveAttachment(uri) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewItem(
    uri: String,
    onRemove: () -> Unit
) {
    val isAudio = uri.contains("audio") || uri.endsWith(".3gp") || uri.endsWith(".mp3") || uri.endsWith(".wav") || uri.endsWith(".m4a")
    val isImage = uri.contains("image") || uri.endsWith(".jpg") || uri.endsWith(".jpeg") || uri.endsWith(".png") || uri.endsWith(".gif")

    Box {
        Card(
            modifier = Modifier.size(96.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = ElevationTokens.card),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isAudio -> ColorTokens.info.copy(alpha = 0.08f)
                    isImage -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isAudio -> {
                        AudioAttachmentPlayer(uri = uri)
                    }
                    isImage -> {
                        AsyncImage(
                            model = uri,
                            contentDescription = stringResource(R.string.attachment_image),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "File attachment",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.attachment_file_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (isAudio || isImage) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when {
                                isAudio -> stringResource(R.string.attachment_audio_label)
                                isImage -> stringResource(R.string.image)
                                else -> stringResource(R.string.attachment_file_label)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(
                    MaterialTheme.colorScheme.error,
                    CircleShape
                )
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Remove attachment",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
