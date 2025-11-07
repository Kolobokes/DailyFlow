package com.dailyflow.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.dailyflow.app.data.dao.*
import com.dailyflow.app.data.model.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [Task::class, Note::class, Category::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DailyFlowDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: DailyFlowDatabase? = null
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE categories ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context, passphrase: String): DailyFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DailyFlowDatabase::class.java,
                    "dailyflow_database"
                )
                .addMigrations(MIGRATION_4_5)
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
