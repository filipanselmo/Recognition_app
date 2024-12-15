package com.example.recognition_app.dto

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: Photo)

    @Query("SELECT * FROM photos")
    suspend fun getAllPhotos(): List<Photo>

}