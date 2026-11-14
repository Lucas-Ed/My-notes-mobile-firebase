package com.example.my_notes.data.remote

import com.example.my_notes.data.model.Category
import com.example.my_notes.data.model.Note
import com.google.firebase.database.DataSnapshot

/**
 * Converte o snapshot de um nó de nota em [Note].
 * A chave do nó no Firebase é usada como [Note.id].
 */
internal fun DataSnapshot.toNote(): Note =
    Note(
        id = key.orEmpty(),
        title = child(FIELD_TITLE).stringValue(),
        content = child(FIELD_CONTENT).stringValue(),
        categoryId = child(FIELD_CATEGORY_ID).stringValue()
    )

/**
 * Converte o snapshot de um nó de categoria em [Category].
 * A chave do nó no Firebase é usada como [Category.id].
 */
internal fun DataSnapshot.toCategory(): Category =
    Category(
        id = key.orEmpty(),
        name = child(FIELD_NAME).stringValue(),
        color = child(FIELD_COLOR).stringValue()
    )

/**
 * Campos gravados no nó da nota. O id não é gravado: ele já é a chave
 * do nó no Firebase e fica implícito na estrutura.
 */
internal fun Note.toFirebaseValues(): Map<String, Any?> =
    mapOf(
        FIELD_TITLE to title,
        FIELD_CONTENT to content,
        FIELD_CATEGORY_ID to categoryId
    )

/** Campos gravados no nó da categoria (sem o id, que é a própria chave). */
internal fun Category.toFirebaseValues(): Map<String, Any?> =
    mapOf(
        FIELD_NAME to name,
        FIELD_COLOR to color
    )

/** Lê um campo de texto do snapshot, tratando nós ausentes ou vazios. */
private fun DataSnapshot.stringValue(): String =
    getValue(String::class.java).orEmpty()

private const val FIELD_TITLE = "title"
private const val FIELD_CONTENT = "content"
private const val FIELD_CATEGORY_ID = "categoryId"
private const val FIELD_NAME = "name"
private const val FIELD_COLOR = "color"
