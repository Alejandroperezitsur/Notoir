package com.example.notesapp_apv_czg.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp_apv_czg.data.Note
import com.example.notesapp_apv_czg.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class NoteViewModel(private val repo: NoteRepository) : ViewModel() {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        repo.getAllNotes()
            .onEach { _notes.value = it }
            .catch { /* handle errors */ }
            .launchIn(viewModelScope)
    }

    fun getNoteById(id: Long) {
        viewModelScope.launch {
            _currentNote.value = repo.getById(id)
        }
    }

    fun clearCurrentNote() {
        _currentNote.value = null
    }

    fun search(q: String) {
        // This is now handled locally in the UI, but the function can be kept for other purposes
        repo.search(q)
            .onEach { _notes.value = it }
            .catch { /* handle */ }
            .launchIn(viewModelScope)
    }

    fun insert(note: Note, onResult: (Long) -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val id = repo.insert(note)
                onResult(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error al guardar la nota"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun update(note: Note) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repo.update(note)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error al actualizar la nota"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun delete(note: Note) {
        viewModelScope.launch {
            try {
                repo.delete(note)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error al eliminar la nota"
            }
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            try {
                repo.update(note.copy(isFavorite = !note.isFavorite))
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error al actualizar favorito"
            }
        }
    }

    fun getFavorites() {
        repo.getFavorites()
            .onEach { _notes.value = it }
            .catch { /* handle */ }
            .launchIn(viewModelScope)
    }

    fun addTag(note: Note, tag: String) {
        if (tag.isNotBlank() && !note.tags.contains(tag)) {
            viewModelScope.launch {
                repo.update(note.copy(tags = note.tags + tag))
            }
        }
    }

    fun removeTag(note: Note, tag: String) {
        viewModelScope.launch {
            repo.update(note.copy(tags = note.tags - tag))
        }
    }

    fun searchByTag(tag: String) {
        repo.getAllNotes()
            .onEach { notes ->
                _notes.value = notes.filter { it.tags.contains(tag) }
            }
            .catch { /* handle */ }
            .launchIn(viewModelScope)
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
