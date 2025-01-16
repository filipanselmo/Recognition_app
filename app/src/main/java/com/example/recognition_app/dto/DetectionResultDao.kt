package com.example.recognition_app.dto

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DetectionResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detectionResult: DetectionResult)

    @Query("SELECT * FROM detection_results WHERE photoId = :photoId")
    fun getResultsForPhoto(photoId: Long?): List<DetectionResult>

    @Query("DELETE FROM detection_results WHERE photoId = :photoId")
    suspend fun deleteResultsForPhoto(photoId: Long)

    @Query("SELECT * FROM detection_results")
    suspend fun getAllResults(): List<DetectionResult>

}