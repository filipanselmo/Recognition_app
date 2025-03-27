package com.example.recognition_app

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.recognition_app.dto.AppDatabase
import com.example.recognition_app.dto.DetectionResult
import com.example.recognition_app.dto.Photo
import com.example.recognition_app.dto.PhotoWithResults
import com.example.recognition_app.dto.Task
import com.example.recognition_app.dto.TaskStatusUpdate
import com.example.recognition_app.http.RetrofitClient
import com.example.recognition_app.screens.PhotoCaptureScreen
import com.example.recognition_app.screens.ResultScreen
import com.example.recognition_app.screens.TaskScreen
import com.example.recognition_app.ui.theme.Regontition_appTheme
import com.example.recognition_app.viemodels.SharedViewModel
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
import retrofit2.awaitResponse
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
                NavHost(navController, startDestination = "task_screen") {
                    composable("task_screen") {
                        TaskScreen(navController)
                    }
                    composable("photo_capture/{taskId}") { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getString("taskId")?.toLongOrNull()
                        PhotoCaptureScreen(
                            navController,
                            database,
                            sharedViewModel,
                            taskId = taskId
                        )
                    }
                    composable("result/{taskId}") { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getString("taskId")
                        ResultScreen(
                            sharedViewModel.bitmap,
                            navController,
                            database,
                            taskId?.toLongOrNull()
                        )
                    }
                }
            }
        }
    }
}


suspend fun fetchTasks(onSuccess: (List<Task>) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.apiService.getTasks().awaitResponse()
            if (response.isSuccessful) {
                onSuccess(response.body() ?: emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

suspend fun updateTaskStatus(taskId: Long, newStatus: String) {
    withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.apiService
                .updateTaskStatus(taskId, TaskStatusUpdate(newStatus))
                .awaitResponse()
            if (!response.isSuccessful) {
                throw Exception("Status update failed")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Отправляет фото на сервер
 */
suspend fun uploadPhoto(context: Context, capturedBitmap: Bitmap): Boolean {
    return withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "captured_image.jpg")
        FileOutputStream(file).use { out ->
            capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        val requestFile = MultipartBody.Part.createFormData(
            "file",
            file.name,
            RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
        )

        try {
            val response = RetrofitClient.apiService.uploadPhoto(requestFile).awaitResponse()
            if (response.isSuccessful) {
                println("File uploaded successfully")
                true
            } else {
                println("Upload failed: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
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

fun fetchPhotosAndResults(
    context: Context,
    database: AppDatabase,
    onResultsFetched: (List<DetectionResult>) -> Unit
) {
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

                            // Получаем последний сохраненный photoId
                            val lastPhotoId = fetchLastPhotoId(database)

                            // Получаем результаты для последнего фото
                            val results = fetchResultsFromDatabase(database, lastPhotoId)

                            // Возвращаем результаты через callback
                            withContext(Dispatchers.Main) {
                                onResultsFetched(results)
                            }
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
            .insert(Photo(filename = photoWithResult.filename))

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
        database.detectionResultDao().getResultsForPhoto(photoId)
    }
}

suspend fun fetchLastPhotoId(database: AppDatabase): Long? {
    return database.photoDao().getLastPhotoId()
}









