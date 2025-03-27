package com.example.recognition_app.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.recognition_app.dto.AppDatabase
import com.example.recognition_app.fetchPhotosAndResults
import com.example.recognition_app.uploadPhoto
import com.example.recognition_app.viemodels.SharedViewModel
import kotlinx.coroutines.launch

/**
 * Функция для создания и последующей работы с фотографиями
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptureScreen(
    navController: NavHostController,
    database: AppDatabase,
    sharedViewModel: SharedViewModel,
    taskId: Long?
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) } // Используем mutableStateOf для состояния
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { result ->
            bitmap = result // Получаем захваченное изображение
        }
    var isLoading by remember { mutableStateOf(false) } // Состояние для прелоадера


    // Верхний тулбар
    TopAppBar(
        title = { Text("Распознавание изображений", fontSize = 18.sp) },
        navigationIcon = {
            IconButton({ }) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = "Меню"
                )
            }
        },
        actions = {
            IconButton({ }) { Icon(Icons.Filled.Info, contentDescription = "О приложении") }
            IconButton({ }) { Icon(Icons.Filled.Search, contentDescription = "Поиск") }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Blue,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Сделайте фотографию")
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Запускаем захват изображения
            launcher.launch()
            // Добавить логику обработки фотографий
        }) {
            Text("Сфотографировать")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            bitmap?.let { capturedBitmap ->
                isLoading = true // Устанавливаем состояние загрузки
                coroutineScope.launch {
                    val uploadSuccess = uploadPhoto(context, capturedBitmap) // Загружаем фото

                    if (uploadSuccess) {
                        sharedViewModel.bitmap = capturedBitmap // Сохраняем Bitmap в ViewModel

                        // Загружаем фото и результаты
                        fetchPhotosAndResults(context, database) { results ->
                            isLoading = false // Сбрасываем состояние загрузки
                            if (results.isNotEmpty()) {
                                navController.navigate("result/$taskId")
                            } else {
                                Toast.makeText(context, "Нет результатов", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    } else {
                        isLoading = false // Сбрасываем состояние загрузки в случае ошибки
                        Toast.makeText(context, "Ошибка загрузки изображения", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } ?: run {
                // Обработка случая, когда bitmap равен null (например, пользователь не сделал фотографию)
                Toast.makeText(context, "Сначала сделайте фотографию", Toast.LENGTH_SHORT).show()
            }
            // Логика перехода на экран результатов
        }) {
            Text("Показать результаты")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(16.dp))


    }

}