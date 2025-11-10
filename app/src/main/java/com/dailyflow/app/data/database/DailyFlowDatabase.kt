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
    entities = [Task::class, Note::class, Category::class, RecurringTemplate::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DailyFlowDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringTemplateDao(): RecurringTemplateDao
    
    companion object {
        @Volatile
        private var INSTANCE: DailyFlowDatabase? = null
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE categories ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_templates (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT,
                        categoryId TEXT,
                        priority TEXT NOT NULL,
                        startDateTime TEXT NOT NULL,
                        durationMinutes INTEGER NOT NULL,
                        recurrenceRule TEXT NOT NULL,
                        reminderEnabled INTEGER NOT NULL,
                        reminderMinutes INTEGER,
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("ALTER TABLE tasks ADD COLUMN seriesId TEXT")
                database.execSQL("ALTER TABLE tasks ADD COLUMN isException INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN originalStartDateTime TEXT")
                database.execSQL("ALTER TABLE tasks ADD COLUMN sequenceNumber INTEGER")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (!hasColumn(database, "notes", "isChecklist")) {
                    database.execSQL("ALTER TABLE notes ADD COLUMN isChecklist INTEGER NOT NULL DEFAULT 0")
                }
                if (!hasColumn(database, "notes", "checklistItems")) {
                    database.execSQL("ALTER TABLE notes ADD COLUMN checklistItems TEXT")
                }
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
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }

        private fun hasColumn(database: SupportSQLiteDatabase, table: String, column: String): Boolean {
            val cursor = database.query("PRAGMA table_info(`$table`)")
            cursor.use {
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && column.equals(cursor.getString(nameIndex), ignoreCase = true)) {
                        return true
                    }
                }
            }
            return false
        }
    }
}
