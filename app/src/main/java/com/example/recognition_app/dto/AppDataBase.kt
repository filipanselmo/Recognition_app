package com.example.recognition_app.dto

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.recognition_app.utils.Converters

@Database(entities = [Photo::class, DetectionResult::class, DetectedObject::class], version = 8, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun detectionResultDao(): DetectionResultDao
    abstract fun detectionObjectDao(): DetectedObjectDao

    companion object {
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
                "photo_database"
            ).fallbackToDestructiveMigration() // Разрешаем разрушительные миграции
                .build()
        }
    }
}