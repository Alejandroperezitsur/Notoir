package com.example.notesapp_apv_czg.data

import java.io.InputStream
import java.io.OutputStream
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class NoteStreamingImporterExporter(
    private val noteDao: NoteDao,
    private val writeEngine: WriteEngine,
    private val logger: StructuredLogger
) {
    private val gson = Gson()

    suspend fun exportNotes(output: OutputStream): Int = withContext(Dispatchers.IO) {
        val writer = output.bufferedWriter()
        val batchSize = 100
        var offset = 0
        var count = 0
        try {
            while (true) {
                val batch = noteDao.getNotesBatch(limit = batchSize, offset = offset)
                if (batch.isEmpty()) {
                    break
                }
                for (note in batch) {
                    val json = gson.toJson(note)
                    writer.write(json)
                    writer.newLine()
                    count++
                }
                offset += batch.size
                if (batch.size < batchSize) {
                    break
                }
            }
            writer.flush()
            logger.info(
                event = "notes_export_success",
                data = mapOf("count" to count.toString())
            )
            count
        } catch (e: Exception) {
            logger.error(
                event = "notes_export_failed",
                throwable = e,
                data = mapOf("count" to count.toString())
            )
            throw e
        }
    }

    suspend fun importNotes(input: InputStream): Int = withContext(Dispatchers.IO) {
        val reader = input.bufferedReader()
        var count = 0
        val delayEvery = 25
        try {
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) continue
                val note = gson.fromJson(line, Note::class.java)
                writeEngine.insert(note.copy(id = 0))
                count++
                if (count % delayEvery == 0) {
                    delay(5)
                }
            }
            logger.info(
                event = "notes_import_success",
                data = mapOf("count" to count.toString())
            )
            count
        } catch (e: Exception) {
            logger.error(
                event = "notes_import_failed",
                throwable = e,
                data = mapOf("count" to count.toString())
            )
            throw e
        }
    }
}
