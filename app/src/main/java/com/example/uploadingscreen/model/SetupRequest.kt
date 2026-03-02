package com.example.uploadingscreen.model

data class SetupRequest(
    val email:String,
    val username: String,
    val avatar:String,
    val password:String,
    val zealId : String,
    val section: String,
    val rollNo:String
)
