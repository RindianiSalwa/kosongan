package com.dapurandia.app.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*
import com.dapurandia.app.model.CloudinaryResponse

interface CloudinaryService {

    @Multipart
    @POST("v1_1/dfstabfqm/image/upload")
    fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody
    ): Call<CloudinaryResponse>
}
