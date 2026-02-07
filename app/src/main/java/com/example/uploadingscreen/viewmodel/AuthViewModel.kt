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

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    // livedata login ka
    private val _loginRes = MutableLiveData<LoginResponse>()
    val loginRes: LiveData<LoginResponse> = _loginRes
    fun login(request: LoginRequest) {
        val mock = false
        viewModelScope.launch {
            if (mock) {
                delay(2000)
                _loginRes.value = LoginResponse(
                    accessToken = "mocktoken_123",
                    user = null, //if in case it returns null model mai "User?" is used
                    message = "Login Success"
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

    // livedata register ka
    private val _signUpRes = MutableLiveData<SignUpResponse>()
    val signUpRes: LiveData<SignUpResponse> = _signUpRes
    fun register(request: SignUpRequest) {
        val mock = false
        viewModelScope.launch {
            if (mock) {
                delay(2000)
                _signUpRes.value = SignUpResponse(

                    message = "User Created",
                    user = null
                )
            } else {
                try {
                    val resp = repo.register(request)
                    if (resp.isSuccessful) {
                        _signUpRes.value = resp.body()
                    }
                } catch (e: Exception) {
                    e.printStackTrace() //used for printing all the error logs

                }
            }
        }
    }
}
