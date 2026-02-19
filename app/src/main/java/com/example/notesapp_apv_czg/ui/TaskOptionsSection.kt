package com.example.notesapp_apv_czg.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notesapp_apv_czg.R
import com.example.notesapp_apv_czg.ui.theme.ColorTokens
import com.example.notesapp_apv_czg.ui.theme.ElevationTokens
import com.example.notesapp_apv_czg.ui.theme.ShapeTokens
import java.text.SimpleDateFormat
import java.util.Calendar

@Composable
fun TaskOptionsSection(
    isTask: Boolean,
    onIsTaskChange: (Boolean) -> Unit,
    isCompleted: Boolean,
    onIsCompletedChange: (Boolean) -> Unit,
    priority: Int,
    onPriorityChange: (Int) -> Unit,
    dueDateMillis: Long?,
    onDueDateChange: (Long?) -> Unit
) {
    NoteTypeSelection(
        isTask = isTask,
        onIsTaskChange = onIsTaskChange
    )

    if (isTask) {
        Spacer(modifier = Modifier.height(16.dp))
        TaskOptions(
            isCompleted = isCompleted,
            onIsCompletedChange = onIsCompletedChange,
            priority = priority,
            onPriorityChange = onPriorityChange,
            dueDateMillis = dueDateMillis,
            onDueDateChange = onDueDateChange
        )
    }
}

@Composable
private fun NoteTypeSelection(isTask: Boolean, onIsTaskChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = !isTask,
            onClick = { onIsTaskChange(false) },
            label = {
                Text(
                    stringResource(R.string.note),
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        FilterChip(
            selected = isTask,
            onClick = { onIsTaskChange(true) },
            label = {
                Text(
                    stringResource(R.string.task),
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Task,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun TaskOptions(
    isCompleted: Boolean, onIsCompletedChange: (Boolean) -> Unit,
    priority: Int, onPriorityChange: (Int) -> Unit,
    dueDateMillis: Long?, onDueDateChange: (Long?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = ElevationTokens.cardLow),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = ShapeTokens.card
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onIsCompletedChange(!isCompleted) }
            ) {
                Checkbox(checked = isCompleted, onCheckedChange = onIsCompletedChange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.completed))
            }
            Spacer(modifier = Modifier.height(16.dp))
            PrioritySelector(priority = priority, onPriorityChange = onPriorityChange)
            Spacer(modifier = Modifier.height(16.dp))
            DateSelector(dueDateMillis = dueDateMillis, onDueDateChange = onDueDateChange)
        }
    }
}

@Composable
private fun PrioritySelector(priority: Int, onPriorityChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.priority))
        Spacer(modifier = Modifier.width(8.dp))
        (1..5).forEach { index ->
            Icon(
                imageVector = if (index <= priority) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                modifier = Modifier.clickable { onPriorityChange(index) },
                tint = if (index <= priority) ColorTokens.favorite else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DateSelector(dueDateMillis: Long?, onDueDateChange: (Long?) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    dueDateMillis?.let { calendar.timeInMillis = it }

    val dateFormat = SimpleDateFormat.getDateInstance()
    val timeFormat = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            onDueDateChange(calendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            onDueDateChange(calendar.timeInMillis)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { datePickerDialog.show() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = dueDateMillis?.let { dateFormat.format(it) } ?: stringResource(R.string.select_date))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { timePickerDialog.show() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = dueDateMillis?.let { timeFormat.format(it) } ?: stringResource(R.string.select_time))
        }
        if (dueDateMillis != null) {
            IconButton(onClick = { onDueDateChange(null) }) {
                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_date))
            }
        }
    }
}
