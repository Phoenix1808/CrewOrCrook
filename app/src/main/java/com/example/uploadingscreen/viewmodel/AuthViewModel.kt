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
import com.example.uploadingscreen.utils.Resource
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    private val _loginRes = MutableLiveData<Resource<LoginResponse>>()
    val loginRes: LiveData<Resource<LoginResponse>> = _loginRes

    fun login(request: LoginRequest) {

        viewModelScope.launch {

            _loginRes.value = Resource.Loading()

            val result = repo.login(request)

            _loginRes.value = result
        }
    }

    private val _signUpRes = MutableLiveData<Resource<SignUpResponse>>()
    val signUpRes: LiveData<Resource<SignUpResponse>> = _signUpRes

    fun register(request: SignUpRequest) {
        viewModelScope.launch {
            _signUpRes.value = Resource.Loading()
            _signUpRes.value = repo.register(request)
        }
    }

}