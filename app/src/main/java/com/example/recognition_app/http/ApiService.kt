package com.example.recognition_app.http

import com.example.recognition_app.dto.Photo
import com.example.recognition_app.dto.PhotoWithResults
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Интерфейс для API, который содержит методы для загрузки и получения изображений.
 */
interface ApiService {

    @Multipart
    @POST("/upload")
    fun uploadPhoto(@Part file: MultipartBody.Part): Call<ResponseBody>

    @GET("/original-photos")
    fun fetchPhotos(): Call<List<Photo>>

    @GET("/photos")
    fun fetchPhotosWithResults(): Call<List<PhotoWithResults>>

}