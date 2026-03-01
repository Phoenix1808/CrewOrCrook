package com.example.uploadingscreen.repository

import com.example.uploadingscreen.utils.Resource
import com.example.uploadingscreen.model.LoginRequest
import com.example.uploadingscreen.model.SignUpRequest
import com.example.uploadingscreen.network.RetrofitClient
import com.example.uploadingscreen.model.LoginResponse
import com.example.uploadingscreen.model.SignUpResponse


class AuthRepository {

    suspend fun login(request: LoginRequest): Resource<LoginResponse> {
        return try {
            val response = RetrofitClient.api.login(request)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Invalid Credentials")
            }

        } catch (e: Exception) {
            Resource.Error("Network Error: ${e.message}")
        }
    }

    suspend fun register(request: SignUpRequest): Resource<SignUpResponse> {
        return try {
            val response = RetrofitClient.api.register(request)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Registration Failed")
            }

        } catch (e: Exception) {
            Resource.Error("Network Error: ${e.message}")
        }
    }
}