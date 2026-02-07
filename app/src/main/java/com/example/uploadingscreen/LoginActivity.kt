package com.example.uploadingscreen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.uploadingscreen.model.LoginRequest
import com.example.uploadingscreen.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel : AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val etusername = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<ImageView>(R.id.btnconfirm)

        btnLogin.setOnClickListener {

            val username = etusername.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (username.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val req = LoginRequest(username, pass)
            viewModel.login(req)

        }
            viewModel.loginRes.observe(this){response ->
                if(response.accessToken!= null){
                    Toast.makeText(this,"Login Success",Toast.LENGTH_SHORT).show()

                    getSharedPreferences("auth",MODE_PRIVATE)
                        .edit()
                        .putString("token",response.accessToken)
                        .apply()
                     startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else{
                    Toast.makeText(this,response.message?:"Login Failed",Toast.LENGTH_SHORT).show()
                }
            }

        }
    }
