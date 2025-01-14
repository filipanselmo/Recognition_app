package com.example.recognition_app.dto

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DetectedObjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detectedObject: DetectedObject)

    @Query("SELECT * FROM detected_objects")
    suspend fun getAllDetectedObjects(): List<DetectedObject>

}
