package com.example.notesapp_apv_czg.data

import com.example.notesapp_apv_czg.security.NoteCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepository(
    private val dao: NoteDao,
    private val logger: StructuredLogger,
    private val crypto: NoteCrypto
) {
    fun getAllNotes(): Flow<List<Note>> = dao.getAllNotes().map { list ->
        list.map { decryptIfNeeded(it) }
    }

    fun getAllTasks(): Flow<List<Note>> = dao.getAllTasks().map { list ->
        list.map { decryptIfNeeded(it) }
    }

    fun getFavorites(): Flow<List<Note>> = dao.getFavorites().map { list ->
        list.map { decryptIfNeeded(it) }
    }

    fun search(q: String): Flow<List<Note>> = dao.search(q).map { list ->
        list.map { decryptIfNeeded(it) }
    }

    suspend fun getById(id: Long): Note? = dao.getById(id)?.let { decryptIfNeeded(it) }

    suspend fun insert(note: Note): Long {
        val titleForLog = if (note.isLocked) "[locked]" else note.title
        logger.info(
            "note_insert_request",
            mapOf(
                "title" to titleForLog,
                "isLocked" to note.isLocked.toString()
            )
        )
        val toStore = encryptIfNeeded(note)
        return dao.insert(toStore)
    }

    suspend fun update(note: Note) {
        logger.info("note_update_request", mapOf("id" to note.id.toString()))
        val toStore = encryptIfNeeded(note)
        dao.update(toStore)
    }

    suspend fun delete(note: Note) {
        logger.info("note_delete_request", mapOf("id" to note.id.toString()))
        dao.delete(note)
    }

    private fun encryptIfNeeded(note: Note): Note {
        if (!note.isLocked) return note
        val encTitle = crypto.encrypt(note.title)
        val encDescription = note.description?.let { crypto.encrypt(it) }
        return note.copy(
            title = encTitle,
            description = encDescription
        )
    }

    private fun decryptIfNeeded(note: Note): Note {
        if (!note.isLocked) return note
        return try {
            val decTitle = crypto.decrypt(note.title)
            val decDescription = note.description?.let { crypto.decrypt(it) }
            note.copy(
                title = decTitle,
                description = decDescription
            )
        } catch (e: Exception) {
            logger.error(
                event = "note_decrypt_failed",
                throwable = e,
                data = mapOf("id" to note.id.toString())
            )
            note.copy(
                title = "[Contenido protegido dañado]",
                description = null
            )
        }
    }
}
