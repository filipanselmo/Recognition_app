package com.example.recognition_app.dto

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "task"
//    , foreignKeys = [ForeignKey(
//    entity = Shelf::class,
//    parentColumns = ["id"],
//    childColumns = ["shelf_id"],
//    onDelete = ForeignKey.CASCADE
//)]
)
data class Task(
    @PrimaryKey (autoGenerate = true) val id: Long = 0,
    val shelfIdentifier: String,
    val category: String,
    val createdAt: String,
    val status: String
)

data class TaskStatusUpdate(val status: String)

data class Recommendation(
    val sku: String,
    val action: String,
    val quantity: Int,
    val priority: String
)