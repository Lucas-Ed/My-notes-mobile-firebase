package com.example.my_notes.data.model

/**
 * Representa uma categoria usada para organizar as notas.
 * Possui um nome e uma cor (formato hex, ex.: "#f6c2d9") para identificação visual.
 *
 * O [id] é a chave única gerada pelo Firebase Realtime Database.
 * Enquanto a categoria ainda não foi salva, o id permanece vazio.
 *
 * @param id Chave única da categoria no Firebase
 * @param name Nome da categoria
 * @param color Cor da categoria no formato hexadecimal (#RRGGBB)
 */
data class Category(
    val id: String = "",
    val name: String = "",
    val color: String = ""
)
