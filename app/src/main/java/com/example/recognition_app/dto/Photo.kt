package com.example.recognition_app.dto

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filename: String,

    )

data class DetectedResults(
    val label: String,
    val confidence: Float
)

data class PhotoWithResults(
    val id: Int,
    val filename: String,
    val detected_objects: List<DetectedResults>
)
