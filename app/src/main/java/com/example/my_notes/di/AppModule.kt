package com.example.my_notes.di

import android.content.Context
import com.example.my_notes.R
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Módulo Hilt responsável por fornecer as dependências do Firebase
 * e dos dispatchers de corrotinas.
 * O Hilt injeta automaticamente onde for necessário.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Fornece a instância única do Firebase Realtime Database.
     *
     * A URL fica em `res/values/strings.xml` (`firebase_database_url`),
     * facilitando a troca caso o banco esteja em outra região.
     */
    @Provides
    @Singleton
    fun provideFirebaseDatabase(@ApplicationContext context: Context): FirebaseDatabase {
        val databaseUrl = context.getString(R.string.firebase_database_url)
        return FirebaseDatabase.getInstance(databaseUrl).apply {
            // Mantém um cache em disco: o app continua funcionando offline
            // e sincroniza as alterações quando a conexão volta.
            runCatching { setPersistenceEnabled(true) }
        }
    }

    /**
     * Fornece o dispatcher usado pelo repositório para operações de IO.
     * Injetado (e não hardcoded) para facilitar os testes unitários.
     */
    @Provides
    fun provideDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
