package com.example.my_notes.data.repository

import com.example.my_notes.data.model.Category
import com.example.my_notes.data.model.Note
import com.example.my_notes.data.remote.snapshotFlow
import com.example.my_notes.data.remote.toCategory
import com.example.my_notes.data.remote.toFirebaseValues
import com.example.my_notes.data.remote.toNote
import com.example.my_notes.util.awaitCompletion
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório central do aplicativo, apoiado no Firebase Realtime Database.
 * Expõe operações de leitura como [Flow] (reativas) e operações de
 * escrita como funções `suspend`, despachadas para a thread de IO.
 *
 * Estrutura usada no banco:
 * ```
 * notes/<push-id>/      { title, content, categoryId }
 * categories/<push-id>/ { name, color }
 * ```
 */
@Singleton
class NotesRepository @Inject constructor(
    database: FirebaseDatabase,
    private val dispatcher: CoroutineDispatcher
) {

    private val notesRef = database.reference.child(NOTES_PATH)
    private val categoriesRef = database.reference.child(CATEGORIES_PATH)

    /** Emite a lista de notas sempre que ela mudar no Firebase. */
    fun observeNotes(): Flow<List<Note>> =
        notesRef.snapshotFlow()
            .map { snapshot ->
                snapshot.children
                    .map { child -> child.toNote() }
                    // Chaves push crescem com o tempo: descendente = mais nova primeiro.
                    .sortedByDescending { note -> note.id }
            }
            .flowOn(dispatcher)

    /** Emite a lista de categorias sempre que ela mudar no Firebase. */
    fun observeCategories(): Flow<List<Category>> =
        categoriesRef.snapshotFlow()
            .map { snapshot ->
                snapshot.children
                    .map { child -> child.toCategory() }
                    .sortedBy { category -> category.id }
            }
            .flowOn(dispatcher)

    /** Insere uma nova nota ou atualiza uma existente. Retorna o id usado. */
    suspend fun saveNote(note: Note): String = withContext(dispatcher) {
        val isNew = note.id.isBlank()
        val reference = if (isNew) notesRef.push() else notesRef.child(note.id)
        val id = if (isNew) {
            reference.key ?: throw IllegalStateException("Não foi possível gerar o id da nota")
        } else {
            note.id
        }
        reference.setValue(note.toFirebaseValues()).awaitCompletion()
        id
    }

    /** Remove uma nota. */
    suspend fun deleteNote(note: Note): Unit = withContext(dispatcher) {
        notesRef.child(note.requireId("nota")).removeValue().awaitCompletion()
    }

    /** Insere uma nova categoria ou atualiza uma existente. Retorna o id usado. */
    suspend fun saveCategory(category: Category): String = withContext(dispatcher) {
        val isNew = category.id.isBlank()
        val reference = if (isNew) categoriesRef.push() else categoriesRef.child(category.id)
        val id = if (isNew) {
            reference.key ?: throw IllegalStateException("Não foi possível gerar o id da categoria")
        } else {
            category.id
        }
        reference.setValue(category.toFirebaseValues()).awaitCompletion()
        id
    }

    /** Remove uma categoria. */
    suspend fun deleteCategory(category: Category): Unit = withContext(dispatcher) {
        categoriesRef.child(category.requireId("categoria")).removeValue().awaitCompletion()
    }

    /** Um registro só pode ser removido se já tiver sido salvo (id não vazio). */
    private fun Note.requireId(entity: String): String {
        require(id.isNotBlank()) { "Não é possível remover uma $entity sem id" }
        return id
    }

    private fun Category.requireId(entity: String): String {
        require(id.isNotBlank()) { "Não é possível remover uma $entity sem id" }
        return id
    }

    private companion object {
        const val NOTES_PATH = "notes"
        const val CATEGORIES_PATH = "categories"
    }
}
