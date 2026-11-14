package com.example.my_notes.data

import com.example.my_notes.data.model.Category
import com.example.my_notes.data.model.Note
import com.example.my_notes.data.remote.toCategory
import com.example.my_notes.data.remote.toFirebaseValues
import com.example.my_notes.data.remote.toNote
import com.google.firebase.database.DataSnapshot
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes dos mapeamentos entre os modelos do app e os nós do
 * Firebase Realtime Database.
 */
class FirebaseMappingTest {

    @Test
    fun `toNote usa a chave do snapshot como id`() {
        val snapshot = fakeSnapshot(
            key = "-Nabc123",
            values = mapOf(
                "title" to "Título",
                "content" to "Conteúdo",
                "categoryId" to "-Ncat1"
            )
        )

        assertEquals(
            Note(
                id = "-Nabc123",
                title = "Título",
                content = "Conteúdo",
                categoryId = "-Ncat1"
            ),
            snapshot.toNote()
        )
    }

    @Test
    fun `toNote trata campos ausentes como vazios`() {
        val snapshot = fakeSnapshot(key = "-N1", values = emptyMap())

        assertEquals(Note(id = "-N1"), snapshot.toNote())
    }

    @Test
    fun `toCategory usa a chave do snapshot como id`() {
        val snapshot = fakeSnapshot(
            key = "-Ncat1",
            values = mapOf("name" to "Estudos", "color" to "#ffffff")
        )

        assertEquals(
            Category(id = "-Ncat1", name = "Estudos", color = "#ffffff"),
            snapshot.toCategory()
        )
    }

    @Test
    fun `toFirebaseValues da nota nao inclui o id`() {
        val note = Note(id = "-N1", title = "T", content = "C", categoryId = "-Ncat1")

        assertEquals(
            mapOf("title" to "T", "content" to "C", "categoryId" to "-Ncat1"),
            note.toFirebaseValues()
        )
    }

    @Test
    fun `toFirebaseValues da categoria nao inclui o id`() {
        val category = Category(id = "-Ncat1", name = "Estudos", color = "#ffffff")

        assertEquals(
            mapOf("name" to "Estudos", "color" to "#ffffff"),
            category.toFirebaseValues()
        )
    }

    private fun fakeSnapshot(key: String?, values: Map<String, String>): DataSnapshot {
        val snapshot = mockk<DataSnapshot>()
        every { snapshot.key } returns key
        every { snapshot.child(any()) } returns stringSnapshot(null)
        values.forEach { (field, value) ->
            every { snapshot.child(field) } returns stringSnapshot(value)
        }
        return snapshot
    }

    private fun stringSnapshot(value: String?): DataSnapshot {
        val snapshot = mockk<DataSnapshot>()
        every { snapshot.getValue(String::class.java) } returns value
        return snapshot
    }
}
