package com.example.uploadingscreen.repository

import com.example.uploadingscreen.model.LoginRequest
import com.example.uploadingscreen.model.SignUpRequest
import com.example.uploadingscreen.network.RetrofitClient


class AuthRepository {
    suspend fun register(request: SignUpRequest)=
        RetrofitClient.api.register(request)

    suspend fun  login(request: LoginRequest)=
        RetrofitClient.api.login(request)
}