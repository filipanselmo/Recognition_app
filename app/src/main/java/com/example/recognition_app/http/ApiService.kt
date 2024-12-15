package com.example.recognition_app.http

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

//интерфейс для API, который содержит методы для загрузки и получения изображений.

interface ApiService {
    @Multipart
    @POST("upload") // Укажите ваш путь к API для загрузки изображения
    fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("user_id") userId: RequestBody): Call<ResponseBody>

    @POST("download") // Укажите ваш путь к API для получения изображения
    fun downloadImage(@Body requestBody: RequestBody): Call<ResponseBody>
//    fun downloadImage(@Path("id") imageId: Int): Call<ResponseBody>
}