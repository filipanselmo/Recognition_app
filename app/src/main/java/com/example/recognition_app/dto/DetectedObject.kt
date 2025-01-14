package com.example.recognition_app.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detected_objects")
data class DetectedObject(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val photoId: Int,
    val className: String,
    val confidence: Float
)
