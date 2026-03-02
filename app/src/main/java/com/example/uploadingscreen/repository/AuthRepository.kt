package com.example.uploadingscreen.repository

import android.util.Log
import com.example.uploadingscreen.utils.Resource
import com.example.uploadingscreen.model.LoginRequest
import com.example.uploadingscreen.model.SignUpRequest
import com.example.uploadingscreen.network.RetrofitClient
import com.example.uploadingscreen.model.LoginResponse
import com.example.uploadingscreen.model.SetupRequest
import com.example.uploadingscreen.model.SetupResponse
import com.example.uploadingscreen.model.SignUpResponse
import retrofit2.Retrofit


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
    suspend fun setup(token:String,request: SetupRequest): Resource<SetupResponse>{
        return try{
            Log.d("SETUP_API", "Calling setup API")
            val response = RetrofitClient.api.setup(
                request,"Bearer $token")
            if(response.isSuccessful && response.body()!=null){
                Log.d("SETUP_API", "Response: ${response.body()}")
                Resource.Success(response.body()!!)
            } else{
                Log.e("SETUP_API", "Error Code: ${response.code()}")
                Resource.Error("SetUp Failed")
            }
        }catch (e:Exception){
            Resource.Error("Network Error: ${e.message}")
        }
    }
}