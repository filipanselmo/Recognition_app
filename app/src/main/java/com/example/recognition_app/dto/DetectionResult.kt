package com.example.recognition_app.dto

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "detection_results",
    foreignKeys = [
        ForeignKey(entity = Photo::class,
            parentColumns = ["id"],
            childColumns = ["photoId"])
    ],
    indices = [
        Index(value = ["photoId"], unique = false)
    ]
)
data class DetectionResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photoId: Long,
    val label: String,
    val confidence: Float,
)
