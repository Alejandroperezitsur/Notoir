package com.example.notesapp_apv_czg.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["isTask", "dueDateMillis"]),
        Index(value = ["createdAtMillis"]),
        Index(value = ["isLocked"])
    ]
)
@TypeConverters(Converters::class)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val isLocked: Boolean = false,
    val isTask: Boolean = false,
    val isCompleted: Boolean = false,
    val dueDateMillis: Long? = null,
    val priority: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val attachmentUris: List<String> = emptyList(),
    val attachmentDescriptions: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList()
)
