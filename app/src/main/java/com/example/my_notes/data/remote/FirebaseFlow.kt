package com.example.my_notes.data.remote

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Converte os eventos de um [Query] do Firebase em um [Flow] reativo.
 *
 * Um novo valor é emitido a cada mudança no nó observado. Quando o
 * coletor é cancelado, o listener é removido do Firebase para evitar
 * vazamentos de memória.
 */
internal fun Query.snapshotFlow(): Flow<DataSnapshot> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(snapshot)
        }

        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }

    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}
