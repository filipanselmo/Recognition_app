package com.example.regontition_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.regontition_app.dto.AppDatabase
import com.example.regontition_app.dto.Photo
import com.example.regontition_app.ui.theme.Regontition_appTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            Regontition_appTheme {
                NavHost(navController, startDestination = "screen1") {
                    composable("screen1") {
                        PhotoCaptureScreen(navController)
                    }
                    composable("result/{result}") { backStackEntry ->
                        val result =
                            backStackEntry.arguments?.getString("result") ?: "Нет результата"
                        ResultScreen(result, navController)
                    }
                }
            }
        }
    }
}


// Функция для создания и последующей работы с фотографиями
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptureScreen(navController: NavHostController) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) } // Используем mutableStateOf для состояния
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { result ->
            bitmap = result // Получаем захваченное изображение
        }


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
//            // Создаем Intent для захвата изображения
//            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//            try {
//                // Проверяем, есть ли приложение, способное обработать этот Intent
//                context.startActivity(takePictureIntent)
//            } catch (e: ActivityNotFoundException) {
//                e.printStackTrace()
//            }

            // Добавить логику обработки фотографий
        }) {
            Text("Сфотографировать")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            bitmap?.let { capturedBitmap ->
                coroutineScope.launch {
                    savePhoto(capturedBitmap, context)
                    // Переход на экран результатов после сохранения фотографии
                    navController.navigate("result/${System.currentTimeMillis()}")
                }
            } ?: run {
                // Обработка случая, когда bitmap равен null (например, пользователь не сделал фотографию)
                Toast.makeText(context, "Сначала сделайте фотографию", Toast.LENGTH_SHORT).show()
            }

//            if(bitmap!= null) {
//                coroutineScope.launch {
//                    savePhoto(bitmap,context)
//                    // Здесь заменить "Ваш результат" на реальный результат обработки фотографии.
//                    val result = "Ваш результат"
//                    navController.navigate("result/$result")
//                }
//            } else {
//                // Обработка случая, когда bitmap равен null (например, пользователь не сделал фотографию)
//                // Можно показать сообщение об ошибке
//            }
            // Логика перехода на экран результатов
        }) {
            Text("Показать результаты")
        }
    }
}

// Отображение результатов обработки
@Composable
fun ResultScreen(result: String, navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Результат распознавания:")
        Spacer(modifier = Modifier.height(16.dp))
        Text(result)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            navController.popBackStack()
        }) {
            Text("Назад")
        }
    }
}


// Функция для сохранения фотографии, с целью последующей ее обработки Yolo
suspend fun savePhoto(bitmap: Bitmap, context: Context) {
    // Сохраните изображение в файловой системе
    val file = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "photo_${System.currentTimeMillis()}.jpg"
    )
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }

//    // Сохраните путь к изображению в базе данных
    val photo = Photo(imagePath = file.absolutePath)
    val db = AppDatabase.getDatabase(context)
    db.photoDao().insert(photo)
}

// Функция для загрузки фотографии
suspend fun loadPhoto(context: Context): List<Bitmap> {
    val db = AppDatabase.getDatabase(context)
    val photos = db.photoDao().getAllPhotos()

    return photos.map { photo ->
        BitmapFactory.decodeFile(photo.imagePath)
    }
}


