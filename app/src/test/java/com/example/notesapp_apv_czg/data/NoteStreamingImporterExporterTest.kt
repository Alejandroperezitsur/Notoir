package com.example.notesapp_apv_czg.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private class InMemoryNoteDao : NoteDao {
    val inserted = mutableListOf<Note>()

    override fun getAllNotes() = kotlinx.coroutines.flow.MutableStateFlow<List<Note>>(inserted).asStateFlow()

    override suspend fun getNotesBatch(limit: Int, offset: Int): List<Note> {
        return inserted.sortedByDescending { it.createdAtMillis }
            .drop(offset)
            .take(limit)
    }
    override fun getAllTasks() = getAllNotes()
    override fun getFavorites() = getAllNotes()
    override fun search(q: String) = getAllNotes()
    override suspend fun getById(id: Long): Note? = inserted.find { it.id == id }

    override suspend fun insert(note: Note): Long {
        val nextId = (inserted.size + 1).toLong()
        val persisted = note.copy(id = nextId)
        inserted.add(persisted)
        return nextId
    }

    override suspend fun update(note: Note) {
        val index = inserted.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            inserted[index] = note
        }
    }

    override suspend fun delete(note: Note) {
        inserted.removeIf { it.id == note.id }
    }
}

private class NoopLogger : StructuredLogger {
    override fun info(event: String, data: Map<String, String>) {}
    override fun error(event: String, throwable: Throwable?, data: Map<String, String>) {}
}

class NoteStreamingImporterExporterTest {
    @Test
    fun `export and import notes using streaming`() = runBlocking {
        val noteDao = InMemoryNoteDao()
        val importerExporter = NoteStreamingImporterExporter(
            noteDao = noteDao,
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
