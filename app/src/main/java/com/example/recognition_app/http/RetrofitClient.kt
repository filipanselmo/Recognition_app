package com.example.recognition_app.http

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// объект Retrofit для выполнения запросов.
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:5000" // Укажите базовый URL вашего сервера

    private val client = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}