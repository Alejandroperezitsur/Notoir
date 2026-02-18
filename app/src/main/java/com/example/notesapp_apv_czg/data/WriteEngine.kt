package com.example.notesapp_apv_czg.data

import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WriteEngine(
    private val noteDao: NoteDao,
    private val offlineWriteDao: OfflineWriteDao,
    private val logger: StructuredLogger,
    coroutineScope: CoroutineScope? = null
) {
    private val gson = Gson()
    private val scope = coroutineScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channel = Channel<OfflineWriteOperation>(capacity = 64)

    private val maxRetries = 3
    private val statusPending = "PENDING"
    private val statusProcessing = "PROCESSING"
    private val statusFailed = "FAILED"

    init {
        scope.launch { processQueue() }
        scope.launch { bootstrapPendingOperations() }
        scope.launch { cleanupFailed() }
    }

    suspend fun insert(note: Note): Long {
        val id = noteDao.insert(note)
        val persisted = note.copy(id = id)
        enqueueOperation("INSERT", id, persisted)
        return id
    }

    suspend fun update(note: Note) {
        noteDao.update(note)
        enqueueOperation("UPDATE", note.id, note)
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
        enqueueOperation("DELETE", note.id, note)
    }

    private suspend fun enqueueOperation(
        operationType: String,
        noteId: Long?,
        note: Note?
    ) {
        val currentCount = offlineWriteDao.count()
        if (currentCount > 500) {
            cleanupFailed()
            val afterCleanup = offlineWriteDao.count()
            if (afterCleanup > 500) {
                logger.error(
                    event = "offline_write_table_limit_reached",
                    throwable = null,
                    data = mapOf(
                        "count" to afterCleanup.toString()
                    )
                )
                return
            }
        }
        val payload = note?.let { gson.toJson(it) }
        val op = OfflineWriteOperation(
            operationType = operationType,
            noteId = noteId,
            notePayload = payload
        )
        val id = offlineWriteDao.enqueue(op)
        val stored = op.copy(id = id)
        logger.info(
            event = "offline_write_enqueued",
            data = mapOf(
                "id" to id.toString(),
                "type" to operationType,
                "noteId" to (noteId?.toString() ?: "null")
            )
        )
        channel.send(stored)
    }

    private suspend fun bootstrapPendingOperations() {
        try {
            var total = 0
            while (true) {
                val batch = offlineWriteDao.getPending(limit = 64)
                if (batch.isEmpty()) {
                    break
                }
                total += batch.size
                for (operation in batch) {
                    channel.send(operation)
                }
                if (batch.size < 64) {
                    break
                }
            }
            logger.info(
                event = "offline_write_bootstrap",
                data = mapOf("count" to total.toString())
            )
        } catch (e: Exception) {
            logger.error("offline_write_bootstrap_failed", e)
        }
    }

    private suspend fun cleanupFailed() {
        try {
            offlineWriteDao.deleteFailed(maxRetries)
            logger.info(event = "offline_write_cleanup_failed", data = emptyMap())
        } catch (e: Exception) {
            logger.error("offline_write_cleanup_failed_error", e)
        }
    }

    private suspend fun processQueue() {
        while (scope.isActive) {
            val operation = channel.receive()
            processOperation(operation)
        }
    }

    @VisibleForTesting
    internal suspend fun processForTest(operation: OfflineWriteOperation) {
        processOperation(operation)
    }

    private suspend fun processOperation(operation: OfflineWriteOperation) {
        val current = offlineWriteDao.getById(operation.id)
        if (current == null) {
            logger.info(
                event = "offline_write_missing",
                data = mapOf("id" to operation.id.toString())
            )
            return
        }
        if (current.status != statusPending) {
            logger.info(
                event = "offline_write_skip_non_pending",
                data = mapOf(
                    "id" to current.id.toString(),
                    "status" to current.status
                )
            )
            return
        }
        val updatedRows = offlineWriteDao.markProcessingIfPending(
            id = current.id,
            pending = statusPending,
            processing = statusProcessing
        )
        if (updatedRows == 0) {
            logger.info(
                event = "offline_write_skip_mark_processing_failed",
                data = mapOf("id" to current.id.toString())
            )
            return
        }
        try {
            logger.info(
                event = "offline_write_process_start",
                data = mapOf(
                    "id" to current.id.toString(),
                    "type" to current.operationType,
                    "noteId" to (current.noteId?.toString() ?: "null"),
                    "retry" to current.retryCount.toString()
                )
            )

            logger.info(
                event = "offline_write_process_apply_local",
                data = mapOf(
                    "id" to current.id.toString(),
                    "type" to current.operationType
                )
            )

            offlineWriteDao.deleteById(current.id)
            logger.info(
                event = "offline_write_process_success",
                data = mapOf("id" to current.id.toString())
            )
        } catch (e: Exception) {
            handleFailure(current.id, e)
        }
    }

    private suspend fun handleFailure(
        id: Long,
        throwable: Throwable
    ) {
        val before = offlineWriteDao.getById(id)
        val currentRetry = before?.retryCount ?: 0
        logger.error(
            event = "offline_write_process_failed",
            throwable = throwable,
            data = mapOf(
                "id" to id.toString(),
                "retry" to currentRetry.toString()
            )
        )
        offlineWriteDao.incrementRetry(id)
        val updated = offlineWriteDao.getById(id)
        val nextRetry = updated?.retryCount ?: (currentRetry + 1)
        if (nextRetry >= maxRetries) {
            offlineWriteDao.updateStatus(id, statusFailed)
            logger.info(
                event = "offline_write_mark_failed",
                data = mapOf(
                    "id" to id.toString(),
                    "retry" to nextRetry.toString()
                )
            )
        } else {
            offlineWriteDao.updateStatus(id, statusPending)
            val retried = (updated ?: before)?.copy(
                status = statusPending,
                retryCount = nextRetry
            )
            if (retried != null) {
                channel.send(retried)
            }
            logger.info(
                event = "offline_write_retry_scheduled",
                data = mapOf(
                    "id" to id.toString(),
                    "retry" to nextRetry.toString()
                )
            )
        }
    }
}
