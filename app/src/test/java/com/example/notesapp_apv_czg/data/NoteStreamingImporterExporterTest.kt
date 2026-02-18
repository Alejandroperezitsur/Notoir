package com.example.notesapp_apv_czg.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private class InMemoryNoteDao : NoteDao {
    private val notesFlow = MutableStateFlow<List<Note>>(emptyList())

    val inserted = mutableListOf<Note>()

    override fun getAllNotes(): Flow<List<Note>> = notesFlow

    override suspend fun getNotesBatch(limit: Int, offset: Int): List<Note> {
        return inserted.sortedByDescending { it.createdAtMillis }
            .drop(offset)
            .take(limit)
    }
    override fun getAllTasks(): Flow<List<Note>> = notesFlow
    override fun getFavorites(): Flow<List<Note>> = notesFlow
    override fun search(q: String): Flow<List<Note>> = notesFlow
    override suspend fun getById(id: Long): Note? = inserted.find { it.id == id }

    override suspend fun insert(note: Note): Long {
        val nextId = (inserted.size + 1).toLong()
        val persisted = note.copy(id = nextId)
        inserted.add(persisted)
        notesFlow.value = inserted.toList()
        return nextId
    }

    override suspend fun update(note: Note) {
        val index = inserted.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            inserted[index] = note
            notesFlow.value = inserted.toList()
        }
    }

    override suspend fun delete(note: Note) {
        inserted.removeIf { it.id == note.id }
        notesFlow.value = inserted.toList()
    }
}

private class CollectingWriteEngine(
    private val delegate: WriteEngine
) {
    suspend fun insert(note: Note): Long = delegate.insert(note)
}

class NoteStreamingImporterExporterTest {
    @Test
    fun `export and import notes using streaming`() = runBlocking {
        val noteDao = InMemoryNoteDao()
        val offlineDao = InMemoryOfflineWriteDao()
        val noteDaoForEngine = object : NoteDao by noteDao {}
        val writeEngine = WriteEngine(
            noteDao = noteDaoForEngine,
            offlineWriteDao = offlineDao,
            logger = NoopLogger(),
            coroutineScope = this
        )
        val importerExporter = NoteStreamingImporterExporter(
            noteDao = noteDao,
            writeEngine = writeEngine,
            logger = NoopLogger()
        )

        noteDao.insert(
            Note(
                title = "First",
                description = "One"
            )
        )
        noteDao.insert(
            Note(
                title = "Second",
                description = "Two"
            )
        )

        val output = ByteArrayOutputStream()
        val exportedCount = importerExporter.exportNotes(output)
        assertEquals(2, exportedCount)

        val data = output.toByteArray()
        val input = ByteArrayInputStream(data)
        val importedCount = importerExporter.importNotes(input)
        assertEquals(2, importedCount)
    }
}
