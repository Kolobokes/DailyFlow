package com.dailyflow.app.di

import android.content.Context
import com.dailyflow.app.data.database.DailyFlowDatabase
import com.dailyflow.app.data.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): DailyFlowDatabase {
        val passphrase = providePassphrase(context)
        return DailyFlowDatabase.getDatabase(context, passphrase)
    }
    
    @Provides
    fun provideTaskDao(database: DailyFlowDatabase): TaskDao = database.taskDao()
    
    @Provides
    fun provideNoteDao(database: DailyFlowDatabase): NoteDao = database.noteDao()
    
    @Provides
    fun provideCategoryDao(database: DailyFlowDatabase): CategoryDao = database.categoryDao()
    
    @Provides
    fun providePassphrase(@ApplicationContext context: Context): String {
        // In a real app, this should be generated securely and stored in Android Keystore
        // For now, we'll use a simple approach
        return "DailyFlowSecretKey2024"
    }
}
