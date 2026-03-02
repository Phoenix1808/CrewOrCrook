package com.example.uploadingscreen.model

data class SetupResponse (
    val message:String,
    val user : SetupUser
)

data class SetupUser(
    val _id: String,
    val username:String,
    val email:String,
    val avatar:String,
    val zealId:String,
    val rollNo: String,
    val section:String,
    val createdAt: String,
    val updatedAt: String
)
