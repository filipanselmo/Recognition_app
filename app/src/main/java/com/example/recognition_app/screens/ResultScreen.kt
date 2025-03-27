package com.example.recognition_app.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recognition_app.dto.AppDatabase
import com.example.recognition_app.dto.DetectionResult
import com.example.recognition_app.fetchLastPhotoId
import com.example.recognition_app.fetchResultsFromDatabase
import com.example.recognition_app.updateTaskStatus
import kotlinx.coroutines.launch

/**
 * Отображение результатов обработки
 */
@Composable
fun ResultScreen(
    bitmap: Bitmap?,
    navController: NavHostController,
    database: AppDatabase,
    taskId: Long?
) {
    val coroutineScope = rememberCoroutineScope()
    var results by remember { mutableStateOf(emptyList<DetectionResult>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val photoId = fetchLastPhotoId(database)
        results = fetchResultsFromDatabase(database, photoId)
        isLoading = false
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            Text("Загрузка...")
        } else {
            if (results.isNotEmpty()) {
                Text("На фото обнаружено:")
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (bitmap != null) {
                Box(modifier = Modifier.size(300.dp)) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text("Нет изображения для отображения")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (results.isEmpty()) {
                Text("Данные не найдены.")
            } else {
                LazyColumn {
                    items(results) { result ->
                        Card(modifier = Modifier.padding(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Объект: ${result.label}")
                                Text("Доверие: ${result.confidence}")
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Назад")
        }

        if (results.isNotEmpty()) { // Исправленная проверка
            Button(onClick = {
                taskId?.let { id ->
                    coroutineScope.launch {
                        try {
                            updateTaskStatus(id, "completed")
                            navController.popBackStack("task_screen", inclusive = false)
                        } catch (e: Exception) {
                            // Показать ошибку
                        }
                    }
                }
            }) {
                Text("Завершить задание")
            }
        }
    }
}