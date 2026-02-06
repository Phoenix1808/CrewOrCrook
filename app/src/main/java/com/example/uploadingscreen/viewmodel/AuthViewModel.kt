package com.example.uploadingscreen.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uploadingscreen.model.LoginRequest
import com.example.uploadingscreen.model.LoginResponse
import com.example.uploadingscreen.model.SignUpRequest
import com.example.uploadingscreen.model.SignUpResponse
import com.example.uploadingscreen.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthViewModel: ViewModel() {
    private val repo = AuthRepository()

    //livedata login
    private val _loginRes = MutableLiveData<LoginResponse>()
    val loginRes : LiveData<LoginResponse> = _loginRes

    fun login(request: LoginRequest){
        val mock = false

        viewModelScope.launch {
            if (mock) {
                delay(3000)
                _loginRes.value = LoginResponse(
                    success = true,
                    message = "Mock Login Successful"
                )
            } else {
                try {
                    val resp = repo.login(request)
                    if (resp.isSuccessful) {
                        _loginRes.value = resp.body()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    //livedata register
    private val _signUpRes = MutableLiveData<SignUpResponse>()
    val signUpRes : LiveData<SignUpResponse> = _signUpRes

    fun register(request: SignUpRequest) {

        val mock = false
        viewModelScope.launch {
            if (mock) {
                delay(5000)
                _signUpRes.value = SignUpResponse(
                    success = true,
                    message = "Mock Signup Success"
                )
            } else {
                try {
                    val resp = repo.register(request)

                    if (resp.isSuccessful) {
                        _signUpRes.value = resp.body()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}