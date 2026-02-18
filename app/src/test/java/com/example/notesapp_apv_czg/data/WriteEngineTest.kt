package com.example.notesapp_apv_czg.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class InMemoryOfflineWriteDao : OfflineWriteDao {
    private val items = mutableListOf<OfflineWriteOperation>()
    private var nextId = 1L
    var failOnDelete = false

    override suspend fun getById(id: Long): OfflineWriteOperation? {
        return items.firstOrNull { it.id == id }
    }

    override suspend fun getPending(limit: Int): List<OfflineWriteOperation> {
        return items.filter { it.status == "PENDING" }.sortedBy { it.id }.take(limit)
    }

    override suspend fun enqueue(operation: OfflineWriteOperation): Long {
        val assigned = operation.copy(id = nextId++)
        items.add(assigned)
        return assigned.id
    }

    override suspend fun deleteById(id: Long) {
        if (failOnDelete) {
            throw IllegalStateException("Simulated delete failure")
        }
        items.removeAll { it.id == id }
    }

    override suspend fun clear() {
        items.clear()
    }

    override suspend fun deleteFailed(maxRetries: Int) {
        items.removeAll { it.status == "FAILED" && it.retryCount >= maxRetries }
    }

    override suspend fun count(): Int = items.size

    override suspend fun updateStatus(id: Long, status: String) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            val current = items[index]
            items[index] = current.copy(status = status)
        }
    }

    override suspend fun incrementRetry(id: Long) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            val current = items[index]
            items[index] = current.copy(retryCount = current.retryCount + 1)
        }
    }

    fun findById(id: Long): OfflineWriteOperation? = items.firstOrNull { it.id == id }
}

private class RecordingNoteDao : NoteDao {
    val inserted = mutableListOf<Note>()
    val updated = mutableListOf<Note>()
    val deleted = mutableListOf<Note>()

    override fun getAllNotes() = throw UnsupportedOperationException()

    override suspend fun getNotesBatch(limit: Int, offset: Int): List<Note> {
        throw UnsupportedOperationException()
    }
    override fun getAllTasks() = throw UnsupportedOperationException()
    override fun getFavorites() = throw UnsupportedOperationException()
    override fun search(q: String) = throw UnsupportedOperationException()
    override suspend fun getById(id: Long): Note? = null

    override suspend fun insert(note: Note): Long {
        inserted.add(note)
        return (inserted.size).toLong()
    }

    override suspend fun update(note: Note) {
        updated.add(note)
    }

    override suspend fun delete(note: Note) {
        deleted.add(note)
    }
}

private class NoopLogger : StructuredLogger {
    override fun info(event: String, data: Map<String, String>) {}
    override fun error(event: String, throwable: Throwable?, data: Map<String, String>) {}
}

class WriteEngineTest {
    @Test
    fun `insert enqueues offline operation and writes to dao`() = runBlocking {
        val noteDao = RecordingNoteDao()
        val offlineDao = InMemoryOfflineWriteDao()
        val engine = WriteEngine(
            noteDao = noteDao,
            offlineWriteDao = offlineDao,
            logger = NoopLogger(),
            coroutineScope = this
        )

        val note = Note(
            title = "Test",
            description = "Demo"
        )

        val id = engine.insert(note)

        assertEquals(1L, id)
        assertEquals(1, noteDao.inserted.size)
        val pending = offlineDao.getPending(limit = 10)
        assertEquals(1, pending.size)
        assertEquals("INSERT", pending.first().operationType)
        assertEquals("PENDING", pending.first().status)
    }

    @Test
    fun `retry is incremented and failed after three attempts`() = runBlocking {
        val noteDao = RecordingNoteDao()
        val offlineDao = InMemoryOfflineWriteDao()
        offlineDao.failOnDelete = true

        val engine = WriteEngine(
            noteDao = noteDao,
            offlineWriteDao = offlineDao,
            logger = NoopLogger(),
            coroutineScope = this
        )

        val note = Note(
            title = "Retry",
            description = "Failure path"
        )

        engine.insert(note)

        repeat(3) {
            val op = offlineDao.getPending(limit = 1).firstOrNull() ?: return@repeat
            try {
                engine.processForTest(op)
            } catch (_: Exception) {
            }
        }

        val stored = offlineDao.findById(1L)
        assertEquals(3, stored?.retryCount)
        assertEquals("FAILED", stored?.status)
    }

    @Test
    fun `bootstrap re-enqueues pending operations`() = runBlocking {
        val noteDao = RecordingNoteDao()
        val offlineDao = InMemoryOfflineWriteDao()

        offlineDao.enqueue(
            OfflineWriteOperation(
                operationType = "INSERT",
                noteId = 1L,
                notePayload = "{}"
            )
        )

        val engine = WriteEngine(
            noteDao = noteDao,
            offlineWriteDao = offlineDao,
            logger = NoopLogger(),
            coroutineScope = this
        )

        kotlinx.coroutines.yield()

        val pending = offlineDao.getPending(limit = 10)
        assertEquals(1, pending.size)
    }
}
