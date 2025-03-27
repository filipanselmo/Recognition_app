package com.example.recognition_app.dto

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.recognition_app.utils.Converters


@Database(
    entities = [Photo::class, DetectionResult::class, DetectedObject::class, Task::class],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun detectionResultDao(): DetectionResultDao
    abstract fun detectionObjectDao(): DetectedObjectDao
    abstract fun taskDao(): TaskDao

    companion object {
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQL для обновления схемы
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS task (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shelf_id INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY(shelf_id) REFERENCES shelf(id)
                    )
                """)
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "photo_database").addMigrations(MIGRATION_8_9)
//                .fallbackToDestructiveMigration()
                .build()
        }
    }
}