package com.example.notesapp_apv_czg.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_write_operations")
data class OfflineWriteOperation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationType: String,
    val noteId: Long?,
    val notePayload: String?,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val retryCount: Int = 0
)

