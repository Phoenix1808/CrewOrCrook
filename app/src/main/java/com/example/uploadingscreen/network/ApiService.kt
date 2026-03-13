package com.example.uploadingscreen.network


import com.example.uploadingscreen.model.AvailableRoomResponse
import com.example.uploadingscreen.model.CreateRoomResponse
import com.example.uploadingscreen.model.LoginRequest
import com.example.uploadingscreen.model.LoginResponse
import com.example.uploadingscreen.model.RoomLookupResponse
import com.example.uploadingscreen.model.SetupRequest
import com.example.uploadingscreen.model.SetupResponse
import com.example.uploadingscreen.model.SignUpRequest
import com.example.uploadingscreen.model.SignUpResponse

import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ApiService {

    @POST("auth/register")
    suspend fun register(
        @Body request: SignUpRequest
    ): Response<SignUpResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/setup")
    suspend fun setup(
        @Body request: SetupRequest,
        @Header("Authorization") token : String
    ): Response<SetupResponse>

    @POST("room/createNew")
    suspend fun createRoom(
        @Header("Authorization") token: String
    ): Response<CreateRoomResponse>

    @GET("room/available")
    suspend fun getAvailableRooms(
        @Header("Authorization") token: String
    ): Response<List<AvailableRoomResponse>>

    @GET("room/{code}/lookup")
    suspend fun lookUpRoom(
        @Header("Authorization") token : String,
        @Path("code") code: String
    ):Response<RoomLookupResponse>
}