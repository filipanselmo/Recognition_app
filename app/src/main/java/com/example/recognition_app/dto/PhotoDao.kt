package com.example.recognition_app.dto

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: Photo): Long

    @Query("SELECT * FROM photos")
    suspend fun getAllPhotos(): List<Photo>

    // Запрос на получение последнего ID фотографии
    @Query("SELECT MAX(id) FROM photos")
    suspend fun getLastPhotoId(): Long?


}