package com.example.recognition_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import com.example.recognition_app.dto.AppDatabase
import com.example.recognition_app.dto.Photo
import com.example.recognition_app.http.RetrofitClient
import com.example.recognition_app.ui.theme.Regontition_appTheme
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
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
            // Добавить логику обработки фотографий
        }) {
            Text("Сфотографировать")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            bitmap?.let { capturedBitmap ->
                coroutineScope.launch {
//                    savePhoto(capturedBitmap, context)
                    uploadImage(capturedBitmap)
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
            downloadImage(1,context)
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
        Text("Данные изображения:")
        Spacer(modifier = Modifier.height(16.dp))
        Text(result)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            navController.popBackStack()
        }) {
            Text("Назад")
        }
    }
    Button(onClick = {

    }) {
        Text("Показать результаты")
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

    // Сохраните путь к изображению в базе данных
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

fun uploadImage(bitmap: Bitmap) {
    // Сжимаем изображение и преобразуем его в байтовый массив
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()

    // Создаем RequestBody для изображения

    val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray)
    val body = MultipartBody.Part.createFormData("file", "photo.jpg", requestFile)

    // Добавляем user_id в RequestBody
    val userId = 1;
    val userIdRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), "1")

    // Выполняем запрос на загрузку изображения

    RetrofitClient.apiService.uploadImage(body,userIdRequestBody).enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
                // Успешно загружено
                Log.d("UploadImage", "Изображение успешно загружено")
            } else {
                // Ошибка при загрузке
                Log.e("UploadImage", "Ошибка: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            // Ошибка сети или другая ошибка
            Log.e("UploadImage", "Ошибка: ${t.message}")
        }
    })
}

//fun fetchProcessedImages(userId: Int) {
//    // Выполняем запрос на получение обработанных изображений
//    RetrofitClient.apiService.getProcessedImages(userId).enqueue(object : Callback<List<ProcessedImage>> {
//        override fun onResponse(call: Call<List<ProcessedImage>>, response: Response<List<ProcessedImage>>) {
//            if (response.isSuccessful) {
//                // Получаем список обработанных изображений
//                val processedImages = response.body()
//                if (processedImages != null) {
//                    // Обрабатываем полученные изображения
//                    for (image in processedImages) {
//                        Log.d("FetchImages", "Получено изображение: ${image.url}")
//                        // Здесь вы можете обновить UI или сохранить изображения
//                    }
//                } else {
//                    Log.e("FetchImages", "Список обработанных изображений пустой")
//                }
//            } else {
//                Log.e("FetchImages", "Ошибка: ${response.errorBody()?.string()}")
//            }
//        }
//
//        override fun onFailure(call: Call<List<ProcessedImage>>, t: Throwable) {
//            Log.e("FetchImages", "Ошибка: ${t.message}")
//        }
//    })
//}

fun downloadImage(imageId: Int,context: Context) {
    // Создаем RequestBody для идентификатора изображения
    val requestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), imageId.toString())

    // Выполняем запрос на получение изображения
    RetrofitClient.apiService.downloadImage(requestBody).enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
                // Успешно получено изображение
                response.body()?.let { responseBody ->
                    // Сохраняем изображение в файл или обрабатываем его
                    val inputStream = responseBody.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    // Здесь вы можете обновить UI или сохранить изображение
                    Log.d("DownloadImage", "Изображение успешно загружено")

                    // Например, если вы хотите сохранить изображение в файл
                    saveImageToFile(bitmap,"downloaded_image.jpg", context)
                }
            } else {
                // Ошибка при загрузке
                Log.e("DownloadImage", "Ошибка: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            // Ошибка сети или другая ошибка
            Log.e("DownloadImage", "Ошибка: ${t.message}")
        }
    })
}

// Функция для сохранения изображения в файл
private fun saveImageToFile(bitmap: Bitmap, fileName: String, context: Context) {
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)
    FileOutputStream(file).use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        Log.d("SaveImage", "Изображение сохранено: ${file.absolutePath}")
    }
}







