package com.example.my_notes.data

import com.example.my_notes.data.model.Category
import com.example.my_notes.data.model.Note
import com.example.my_notes.data.repository.NotesRepository
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários do [NotesRepository].
 * Verifica leitura reativa, geração de chaves, escrita, exclusão
 * e propagação de erros usando o Firebase simulado com MockK.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesRepositoryTest {

    private val database: FirebaseDatabase = mockk()
    private val root: DatabaseReference = mockk()
    private val notesRef: DatabaseReference = mockk(relaxed = true)
    private val categoriesRef: DatabaseReference = mockk(relaxed = true)

    @Before
    fun setup() {
        every { database.reference } returns root
        every { root.child("notes") } returns notesRef
        every { root.child("categories") } returns categoriesRef
    }

    @Test
    fun `observeNotes emite notas ordenadas da chave mais recente para a mais antiga`() = runTest {
        val listener = slot<ValueEventListener>()
        every { notesRef.addValueEventListener(capture(listener)) } answers { firstArg() }

        val emitted = mutableListOf<List<Note>>()
        val job = launch {
            repository().observeNotes().collect { emitted += it }
        }
        runCurrent()

        listener.captured.onDataChange(
            childrenSnapshot(
                noteSnapshot(key = "a", title = "Antiga"),
                noteSnapshot(key = "c", title = "Nova")
            )
        )
        advanceUntilIdle()

        assertEquals(listOf("c", "a"), emitted.last().map { it.id })
        job.cancelAndJoin()
        verify { notesRef.removeEventListener(listener.captured) }
    }

    @Test
    fun `observeCategories emite categorias em ordem de criacao`() = runTest {
        val listener = slot<ValueEventListener>()
        every { categoriesRef.addValueEventListener(capture(listener)) } answers { firstArg() }

        val emitted = mutableListOf<List<Category>>()
        val job = launch {
            repository().observeCategories().collect { emitted += it }
        }
        runCurrent()

        listener.captured.onDataChange(
            childrenSnapshot(
                categorySnapshot(key = "a", name = "Primeira"),
                categorySnapshot(key = "b", name = "Segunda")
            )
        )
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), emitted.last().map { it.id })
        job.cancelAndJoin()
    }

    @Test
    fun `observeNotes propaga erro do Firebase`() = runTest {
        val listener = slot<ValueEventListener>()
        every { notesRef.addValueEventListener(capture(listener)) } answers { firstArg() }
        val firebaseError = mockk<DatabaseError>()
        every { firebaseError.toException() } returns DatabaseException("Sem permissão")

        val result = async {
            runCatching { repository().observeNotes().first() }
        }
        runCurrent()
        listener.captured.onCancelled(firebaseError)

        assertTrue(result.await().exceptionOrNull() is DatabaseException)
    }

    @Test
    fun `saveNote com id vazio grava em chave gerada pelo push`() = runTest {
        val pushedRef = mockk<DatabaseReference>()
        every { notesRef.push() } returns pushedRef
        every { pushedRef.key } returns "nova-chave"
        every { pushedRef.setValue(any()) } returns successfulTask()

        val id = repository().saveNote(
            Note(title = "T", content = "C", categoryId = "cat")
        )

        assertEquals("nova-chave", id)
        verify {
            pushedRef.setValue(
                mapOf("title" to "T", "content" to "C", "categoryId" to "cat")
            )
        }
    }

    @Test
    fun `saveNote com id existente atualiza o no correspondente`() = runTest {
        val existingRef = mockk<DatabaseReference>()
        every { notesRef.child("n1") } returns existingRef
        every { existingRef.setValue(any()) } returns successfulTask()

        val id = repository().saveNote(
            Note(id = "n1", title = "Editada", content = "C", categoryId = "cat")
        )

        assertEquals("n1", id)
        verify { existingRef.setValue(any()) }
        verify(exactly = 0) { notesRef.push() }
    }

    @Test
    fun `deleteNote remove o no da nota`() = runTest {
        val noteRef = mockk<DatabaseReference>()
        every { notesRef.child("n1") } returns noteRef
        every { noteRef.removeValue() } returns successfulTask()

        repository().deleteNote(Note(id = "n1"))

        verify { noteRef.removeValue() }
    }

    @Test
    fun `deleteNote sem id falha sem tocar no Firebase`() = runTest {
        val result = runCatching { repository().deleteNote(Note(id = "")) }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        verify(exactly = 0) { notesRef.child(any()) }
    }

    @Test
    fun `saveCategory com id vazio grava em chave gerada pelo push`() = runTest {
        val pushedRef = mockk<DatabaseReference>()
        every { categoriesRef.push() } returns pushedRef
        every { pushedRef.key } returns "nova-categoria"
        every { pushedRef.setValue(any()) } returns successfulTask()

        val id = repository().saveCategory(
            Category(name = "Estudos", color = "#ffffff")
        )

        assertEquals("nova-categoria", id)
        verify {
            pushedRef.setValue(mapOf("name" to "Estudos", "color" to "#ffffff"))
        }
    }

    @Test
    fun `saveCategory com id existente atualiza o no correspondente`() = runTest {
        val existingRef = mockk<DatabaseReference>()
        every { categoriesRef.child("c1") } returns existingRef
        every { existingRef.setValue(any()) } returns successfulTask()

        repository().saveCategory(
            Category(id = "c1", name = "Estudos", color = "#000000")
        )

        verify { existingRef.setValue(any()) }
    }

    @Test
    fun `deleteCategory remove o no da categoria`() = runTest {
        val categoryRef = mockk<DatabaseReference>()
        every { categoriesRef.child("c1") } returns categoryRef
        every { categoryRef.removeValue() } returns successfulTask()

        repository().deleteCategory(Category(id = "c1"))

        verify { categoryRef.removeValue() }
    }

    @Test
    fun `saveNote propaga falha de escrita do Firebase`() = runTest {
        val pushedRef = mockk<DatabaseReference>()
        every { notesRef.push() } returns pushedRef
        every { pushedRef.key } returns "x"
        every { pushedRef.setValue(any()) } returns failedTask(DatabaseException("Offline"))

        val result = runCatching {
            repository().saveNote(Note(title = "T", content = "C", categoryId = "cat"))
        }

        assertTrue(result.exceptionOrNull() is DatabaseException)
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private fun TestScope.repository(): NotesRepository =
        NotesRepository(database, UnconfinedTestDispatcher(testScheduler))

    private fun childrenSnapshot(vararg children: DataSnapshot): DataSnapshot {
        val snapshot = mockk<DataSnapshot>()
        every { snapshot.children } returns children.toList()
        return snapshot
    }

    private fun noteSnapshot(key: String, title: String): DataSnapshot {
        val snapshot = mockk<DataSnapshot>()
        every { snapshot.key } returns key
        every { snapshot.child("title") } returns stringSnapshot(title)
        every { snapshot.child("content") } returns stringSnapshot("Conteúdo")
        every { snapshot.child("categoryId") } returns stringSnapshot("cat")
        return snapshot
    }

    private fun categorySnapshot(key: String, name: String): DataSnapshot {
        val snapshot = mockk<DataSnapshot>()
        every { snapshot.key } returns key
        every { snapshot.child("name") } returns stringSnapshot(name)
        every { snapshot.child("color") } returns stringSnapshot("#ffffff")
        return snapshot
    }

    private fun stringSnapshot(value: String?): DataSnapshot {
        val snapshot = mockk<DataSnapshot>()
        every { snapshot.getValue(String::class.java) } returns value
        return snapshot
    }

    private fun successfulTask(): Task<Void> {
        val task = mockk<Task<Void>>()
        every { task.isSuccessful } returns true
        every { task.exception } returns null
        every { task.result } returns null
        every { task.addOnCompleteListener(any()) } answers {
            firstArg<OnCompleteListener<Void>>().onComplete(task)
            task
        }
        return task
    }

    private fun failedTask(error: Exception): Task<Void> {
        val task = mockk<Task<Void>>()
        every { task.isSuccessful } returns false
        every { task.exception } returns error
        every { task.addOnCompleteListener(any()) } answers {
            firstArg<OnCompleteListener<Void>>().onComplete(task)
            task
        }
        return task
    }
}
