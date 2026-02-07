package com.example.uploadingscreen.model

data class LoginResponse(
    val message: String,
    val accessToken : String,
    val user : User?
)
