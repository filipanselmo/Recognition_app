package com.example.recognition_app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recognition_app.dto.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskItem(task: Task, onTaskClick: (Long) -> Unit) {
    // Форматирование даты с использованием SimpleDateFormat
    val formattedDate = remember(task.createdAt) {
        task.createdAt.toFormattedDate("dd.MM.yyyy HH:mm")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onTaskClick(task.id) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Стеллаж: ${task.shelfIdentifier}", fontSize = 16.sp)
            Text("Категория: ${task.category}", fontSize = 14.sp)
            Text("Дата: $formattedDate", fontSize = 14.sp) // Используем formattedDate
            Text(
                "Статус: ${task.status}",
                fontSize = 14.sp,
                color = when (task.status) {
                    "pending" -> Color.Yellow
                    "in_progress" -> Color.Blue
                    "completed" -> Color.Green
                    else -> Color.Gray
                }
            )
        }
    }
}

// Расширение для форматирования даты
fun String.toFormattedDate(pattern: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat(pattern, Locale.getDefault())
        val date = inputFormat.parse(this)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        this // Возвращаем исходную строку при ошибке
    }
}