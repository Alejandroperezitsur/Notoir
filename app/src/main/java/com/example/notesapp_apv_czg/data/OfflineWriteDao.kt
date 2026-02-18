package com.example.notesapp_apv_czg.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OfflineWriteDao {
    @Query("SELECT * FROM offline_write_operations WHERE status = 'PENDING' ORDER BY id ASC LIMIT :limit")
    suspend fun getPending(limit: Int): List<OfflineWriteOperation>

    @Query("SELECT * FROM offline_write_operations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): OfflineWriteOperation?

    @Insert
    suspend fun enqueue(operation: OfflineWriteOperation): Long

    @Query("DELETE FROM offline_write_operations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM offline_write_operations")
    suspend fun clear()

    @Query("DELETE FROM offline_write_operations WHERE status = 'FAILED' AND retryCount >= :maxRetries")
    suspend fun deleteFailed(maxRetries: Int)

    @Query("SELECT COUNT(*) FROM offline_write_operations")
    suspend fun count(): Int

    @Query("UPDATE offline_write_operations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE offline_write_operations SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query(
        """
        UPDATE offline_write_operations
        SET status = :processing
        WHERE id = :id AND status = :pending
        """
    )
    suspend fun markProcessingIfPending(
        id: Long,
        pending: String,
        processing: String
    ): Int
}
