package com.example.notesapp_apv_czg.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val dao: NoteDao,
    private val writeEngine: WriteEngine,
    private val logger: StructuredLogger
) {
    fun getAllNotes(): Flow<List<Note>> = dao.getAllNotes()
    fun getAllTasks(): Flow<List<Note>> = dao.getAllTasks()
    fun getFavorites(): Flow<List<Note>> = dao.getFavorites()
    fun search(q: String): Flow<List<Note>> = dao.search(q)

    suspend fun getById(id: Long): Note? = dao.getById(id)

    suspend fun insert(note: Note): Long {
        logger.info("note_insert_request", mapOf("title" to note.title))
        return writeEngine.insert(note)
    }

    suspend fun update(note: Note) {
        logger.info("note_update_request", mapOf("id" to note.id.toString()))
        writeEngine.update(note)
    }

    suspend fun delete(note: Note) {
        logger.info("note_delete_request", mapOf("id" to note.id.toString()))
        writeEngine.delete(note)
    }
}
