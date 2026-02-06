package com.example.uploadingscreen.network


import com.example.uploadingscreen.model.LoginRequest
import com.example.uploadingscreen.model.LoginResponse
import com.example.uploadingscreen.model.SignUpRequest
import com.example.uploadingscreen.model.SignUpResponse

import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Body

interface ApiService {

    @POST("auth/register")
    suspend fun register(
        @Body request: SignUpRequest
    ): Response<SignUpResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

}