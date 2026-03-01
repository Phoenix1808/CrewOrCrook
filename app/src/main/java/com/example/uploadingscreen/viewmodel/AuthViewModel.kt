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
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()


    private val _loginRes = MutableLiveData<LoginResponse>()
    val loginRes: LiveData<LoginResponse> = _loginRes

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    fun login(request: LoginRequest) {

        _loading.value = true
        viewModelScope.launch {

                try {

                    val resp = repo.login(request)

                    if (resp.isSuccessful && resp.body()!=null) {
                        _loginRes.value = resp.body()
                    } else {
                        _loginRes.value = LoginResponse(
                            accessToken = null,
                            user = null,
                            message = "Invalid Credentials"
                        )
                    }
                } catch (e: Exception) {
                    _loginRes.value = LoginResponse(
                        accessToken = null,
                        user = null,
                        message = "Network Error: ${e.message}"
                    )
                } finally {
                    _loading.value = false
                }
            }
    }


    private val _signUpRes = MutableLiveData<SignUpResponse>()
    val signUpRes: LiveData<SignUpResponse> = _signUpRes

    private val _signLoad = MutableLiveData<Boolean>()
    val signLoad : LiveData<Boolean> = _signLoad

    fun register(request: SignUpRequest) {
        _signLoad.value = true

        viewModelScope.launch {

                try {
                    val resp = repo.register(request)
                    if (resp.isSuccessful) {
                        _signUpRes.value = resp.body()
                    } else {
                        _signUpRes.value = SignUpResponse(
                            message = "Registration Failed",
                            user = null
                        )
                    }
                } catch (e: Exception) {
                    _signUpRes.value = SignUpResponse(
                        message = "Network Error: ${e.message}",
                        user = null
                    )
                }
                finally {
                    _signLoad.value = false
                }
            }
        }
    }

