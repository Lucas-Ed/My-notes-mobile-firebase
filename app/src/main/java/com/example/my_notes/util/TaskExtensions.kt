package com.example.my_notes.util

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspende até a conclusão de uma [Task] do Google Play Services,
 * retomando com exceção quando a operação falhar.
 *
 * Usada para transformar as escritas do Firebase (`setValue`,
 * `removeValue`, ...) em chamadas `suspend` normais.
 */
internal suspend fun Task<*>.awaitCompletion(): Unit =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Operação do Firebase falhou")
                )
            }
        }
    }
