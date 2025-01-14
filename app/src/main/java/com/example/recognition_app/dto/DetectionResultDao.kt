package com.example.recognition_app.dto

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DetectionResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detectionResult: DetectionResult)

    @Query("SELECT * FROM detection_results WHERE photoId = :photoId")
//    fun getResultsForPhoto(photoId: Int): LiveData<List<DetectionResult>>
//    fun getResultsForPhoto(photoId: Long): LiveData<List<DetectionResult>>
    fun getResultsForPhoto(photoId: Long?):List<DetectionResult>

    @Query("DELETE FROM detection_results WHERE photoId = :photoId")
//    suspend fun deleteResultsForPhoto(photoId: Int)
    suspend fun deleteResultsForPhoto(photoId: Long)

    @Query("SELECT * FROM detection_results")
    suspend fun getAllResults(): List<DetectionResult>

//    @Query("SELECT * FROM detection_results ORDER BY createdAt DESC")
//    fun getAllResults(): LiveData<List<DetectionResult>>
}