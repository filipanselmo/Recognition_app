package com.example.recognition_app.http

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Объект Retrofit для выполнения запросов.
 */
object RetrofitClient {

    // Для отладки и тестирования на эмуляторе использовать этот хост
//    private const val BASE_URL = "http://10.0.2.2:5000"

    // Для отладки и и тестирования на реальном устройстве использовать IP-адрес своего компьютера.
    private const val BASE_URL = "http://192.168.0.10:5000/"

    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}