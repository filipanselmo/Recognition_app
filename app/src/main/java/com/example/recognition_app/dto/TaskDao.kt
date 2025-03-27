package com.example.recognition_app.dto

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

    @Dao
    interface TaskDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insert(task: Task)

        @Update
        suspend fun update(task: Task)

        @Query("SELECT * FROM task")
        suspend fun getAllTasks(): List<Task>

        @Query("SELECT * FROM task WHERE id = :taskId")
        suspend fun getTaskById(taskId: Long): Task?

        @Query("DELETE FROM task")
        suspend fun clearTasks()
    }