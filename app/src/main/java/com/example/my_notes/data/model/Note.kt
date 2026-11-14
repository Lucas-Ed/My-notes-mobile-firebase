package com.example.my_notes.data.model

/**
 * Representa uma nota (post-it) criada pelo usuário.
 * Cada nota pertence a uma categoria e possui título e conteúdo.
 *
 * O [id] é a chave única gerada pelo Firebase Realtime Database.
 * Enquanto a nota ainda não foi salva, o id permanece vazio.
 *
 * @param id Chave única da nota no Firebase
 * @param title Título da nota
 * @param content Conteúdo/texto da nota
 * @param categoryId Chave da categoria à qual a nota pertence
 */
data class Note(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val categoryId: String = ""
)
