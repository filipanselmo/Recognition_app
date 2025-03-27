package com.example.recognition_app.http

import com.example.recognition_app.dto.Photo
import com.example.recognition_app.dto.PhotoWithResults
import com.example.recognition_app.dto.Recommendation
import com.example.recognition_app.dto.Task
import com.example.recognition_app.dto.TaskStatusUpdate
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Интерфейс для API, который содержит методы для загрузки и получения изображений и заданий.
 */
interface ApiService {

    @Multipart
    @POST("/upload")
    fun uploadPhoto(@Part file: MultipartBody.Part): Call<ResponseBody>

    @GET("/original-photos")
    fun fetchPhotos(): Call<List<Photo>>

    @GET("/photos")
    fun fetchPhotosWithResults(): Call<List<PhotoWithResults>>

    @GET("/api/tasks")
    fun getTasks(): Call<List<Task>>

    @PATCH("/api/tasks/{taskId}")
    suspend fun updateTaskStatus(
        @Path("taskId") taskId: Long,
        @Body statusUpdate: TaskStatusUpdate
    ): Call<Response<Unit>>

    @GET("/api/recommendations/{taskId}")
    suspend fun getRecommendations(@Path("taskId") taskId: Long): List<Recommendation>

}