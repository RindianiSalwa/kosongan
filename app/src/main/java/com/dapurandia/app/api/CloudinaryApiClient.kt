package com.dapurandia.app.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CloudinaryApiClient {

    private const val BASE_URL = "https://api.cloudinary.com/"

    private val client = OkHttpClient.Builder().build()

    val instance: CloudinaryService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudinaryService::class.java)
    }
}
