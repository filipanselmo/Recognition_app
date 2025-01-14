package com.example.recognition_app

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.example.recognition_app.dto.AppDatabase
import com.example.recognition_app.dto.DetectionResult
import com.example.recognition_app.dto.Photo
import com.example.recognition_app.dto.PhotoWithResults
import com.example.recognition_app.http.RetrofitClient
import com.example.recognition_app.ui.theme.Regontition_appTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    lateinit var database: AppDatabase

    // Эти данные есть в Python console
    // Raw detection results: image 1/1: 675x1080 11 persons, 12 cars, 1 handbag
    //  Speed: 6.0ms pre-process, 161.6ms inference, 19.0ms NMS per image at shape (1, 3, 416, 640)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Инициализация базы данных
        database = AppDatabase.getDatabase(this)

        setContent {
            val navController = rememberNavController()
            val sharedViewModel: SharedViewModel = viewModel() // Получаем ViewModel
            Regontition_appTheme {
                NavHost(navController, startDestination = "screen1") {
                    composable("screen1") {
                        PhotoCaptureScreen(navController, database, sharedViewModel)
                    }
                    composable("result/{result}") { backStackEntry ->
                        val result =
                            backStackEntry.arguments?.getString("result") ?: "Нет результата"
//                        ResultScreen(result, navController, database)
                        ResultScreen(
                            sharedViewModel.bitmap,
                            navController,
                            database
                        )// Передаем Bitmap из ViewModel
                    }
                }
            }
        }
    }
}

/**
 * Функция для создания и последующей работы с фотографиями
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptureScreen(
    navController: NavHostController,
    database: AppDatabase,
    sharedViewModel: SharedViewModel
) {

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
            // Добавить логику обработки фотографий
        }) {
            Text("Сфотографировать")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            bitmap?.let { capturedBitmap ->
                coroutineScope.launch {
                    uploadPhoto(context, capturedBitmap)
                    sharedViewModel.bitmap = capturedBitmap // Сохраняем Bitmap в ViewModel
                    // Переход на экран результатов после сохранения фотографии
                    navController.navigate("result/${System.currentTimeMillis()}")
                }
            } ?: run {
                // Обработка случая, когда bitmap равен null (например, пользователь не сделал фотографию)
                Toast.makeText(context, "Сначала сделайте фотографию", Toast.LENGTH_SHORT).show()
            }
            // Логика перехода на экран результатов
        }) {
            Text("Отправить на обработку")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            fetchPhotosAndResults(context, database)
//            fetchPhotos(context, database)
        }) {
            Text("Показать результаты")
        }

        Spacer(modifier = Modifier.height(16.dp))

        val imagePath = "${context.filesDir}/test_images/test1.jpg"
        Button(onClick = {
            testUploadPhoto(context, imagePath)
        }) {
            Text("Тестирование YOLO")
        }

    }

}


/**
 * Отображение результатов обработки
 */
@Composable
//fun ResultScreen(imageUri: String?, navController: NavHostController, database: AppDatabase) {
fun ResultScreen(bitmap: Bitmap?, navController: NavHostController, database: AppDatabase) {
    val coroutineScope = rememberCoroutineScope()
    var results by remember { mutableStateOf(emptyList<DetectionResult>()) }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Передаем photoId для получения данных последней фотографии
        val photoId = fetchLastPhotoId(database)
        // Получаем данные из БД
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
                Box(modifier = Modifier.size(300.dp)) { // Установите размер Box по вашему усмотрению
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text("Нет изображения для отображения")
            }

//            Box(
//                modifier = Modifier
//                    .size(200.dp)
//                    .border(2.dp, Color.Gray),
//                contentAlignment = Alignment.Center
//            ) {
//                if (!imageUri.isNullOrEmpty()) {
//                    Image(
//                        painter = rememberImagePainter(imageUri),
//                        contentDescription = "Результат обработки изображения",
//                        modifier = Modifier.fillMaxSize()
//                    )
//                } else {
//                    Text("Изображение отсутствует")
//                }
//            }
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
        Button(onClick = { navController.popBackStack() }) { Text("Назад") }
    }
}


/**
 * Отправляет фото на сервер
 */
fun uploadPhoto(context: Context, capturedBitmap: Bitmap) {
    val file = File(context.cacheDir, "captured_image.jpg")
    FileOutputStream(file).use { out ->
        capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
    val requestFile = MultipartBody.Part.createFormData(
        "file", file.name,
        RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
    )

    RetrofitClient.apiService.uploadPhoto(requestFile)
        .enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: retrofit2.Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    println("File uploaded successfully")
                } else {
                    println("Upload failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
            }
        })
}

/**
 * Тестовая функция для отладки через эмулятор
 * Отправляет заготовленное изображение на сервер
 */
fun testUploadPhoto(context: Context, imagePath: String) {
    // Создаем файл из переданного пути
    val file = File(imagePath)

    // Проверяем, существует ли файл
    if (!file.exists()) {
        println("File does not exist: $imagePath")
        return
    }

    // Создаем MultipartBody.Part для отправки файла
    val requestFile = MultipartBody.Part.createFormData(
        "file", file.name,
        RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
    )

    // Отправляем файл на сервер
    RetrofitClient.apiService.uploadPhoto(requestFile)
        .enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: retrofit2.Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    println("File uploaded successfully")
                } else {
                    println("Upload failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
            }
        })
}

/**
 * Получает фотографии с сервера
 */
fun fetchPhotos(context: Context, database: AppDatabase) {
    RetrofitClient.apiService.fetchPhotos().enqueue(object : retrofit2.Callback<List<Photo>> {
        override fun onResponse(
            call: Call<List<Photo>>,
            response: retrofit2.Response<List<Photo>>
        ) {
            if (response.isSuccessful) {
                response.body()?.let {
                    savePhotos(it, context, database)
                }
            } else {
                println("Fetch failed: ${response.message()}")
            }
        }

        override fun onFailure(call: Call<List<Photo>>, t: Throwable) {
            t.printStackTrace()
        }
    })
}

/**
 * Сохраняет фотографии в локальную БД МУ
 */
fun savePhotos(photos: List<Photo>, context: Context, database: AppDatabase) {
    // Сохранение фотографий в локальную БД
    CoroutineScope(Dispatchers.IO).launch {
        photos.forEach { photo ->
            database.photoDao().insert(photo)
        }
        println("Photos saved to local database.")
    }
}

/**
 * Получает информацию о результатах распознавания изображений на фотографии
 */
fun fetchPhotosAndResults(context: Context, database: AppDatabase) {
    RetrofitClient.apiService.fetchPhotosWithResults()
        .enqueue(object : Callback<List<PhotoWithResults>> {
            override fun onResponse(
                call: Call<List<PhotoWithResults>>,
                response: Response<List<PhotoWithResults>>
            ) {
                if (response.isSuccessful) {
                    CoroutineScope(Dispatchers.IO).launch {
                        response.body()?.let { photosWithResults ->
                            savePhotosAndResults(photosWithResults, context, database)
                        }
                    }

                } else {
                    println("Fetch failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<PhotoWithResults>>, t: Throwable) {
                t.printStackTrace()
            }
        })
}

/**
 * Сохраняет связку фотографий и результата распознавания
 */
suspend fun savePhotosAndResults(
    photosWithResults: List<PhotoWithResults>,
    context: Context,
    database: AppDatabase
) {
    for (photoWithResult in photosWithResults) {
        if (photoWithResult == null || photoWithResult.filename.isNullOrBlank()) continue

        // Сохраняем фото
        val photoId = database.photoDao()
            .insert(Photo(filename = photoWithResult.filename /*content = photoWithResult.content*/))

        // Сохраняем детектированные объекты
        val detectedObjects = photoWithResult.detected_objects ?: emptyList()
        for (detectedObject in detectedObjects) {
            if (detectedObject != null && detectedObject.label.isNotEmpty() && detectedObject.confidence > 0f) {
                val detectionResult = DetectionResult(
                    photoId = photoId,
                    label = detectedObject.label,
                    confidence = detectedObject.confidence
                )
                database.detectionResultDao().insert(detectionResult)
            }
        }
    }
}

suspend fun fetchResultsFromDatabase(database: AppDatabase, photoId: Long?): List<DetectionResult> {
    return withContext(Dispatchers.IO) {
//        database.detectionResultDao().getAllResults()
        database.detectionResultDao().getResultsForPhoto(photoId)
    }
}

suspend fun fetchLastPhotoId(database: AppDatabase): Long? {
    return database.photoDao().getLastPhotoId()
}









