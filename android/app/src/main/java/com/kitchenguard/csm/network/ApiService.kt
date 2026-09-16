package com.kitchenguard.csm.network

import com.kitchenguard.csm.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiService {

    @POST("api/barcode/scan")
    fun scanBarcode(@Body request: BarcodeRequest): Call<BarcodeResponse>

    @POST("api/vision/scan-ingredient")
    fun scanIngredient(@Body request: VisionRequest): Call<VisionResponse>

    @POST("api/ocr/scan-scale")
    fun scanScale(@Body request: OCRScaleRequest): Call<OCRScaleResponse>
}

object RetrofitClient {
    // 10.0.2.2 adalah IP gateway localhost dari Android Emulator ke PC host
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val instance: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}
