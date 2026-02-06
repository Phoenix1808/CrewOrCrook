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

        val etZeal = findViewById<EditText>(R.id.etZealId)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<ImageView>(R.id.btnconfirm)

        btnLogin.setOnClickListener {

            val zeal = etZeal.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (zeal.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val req = LoginRequest(zeal, pass)
            viewModel.login(req)

        }
            viewModel.loginRes.observe(this){response ->
                if(response.success){
                    Toast.makeText(this,"Login Success",Toast.LENGTH_SHORT).show()

                    val prefs = getSharedPreferences("users_prefs", MODE_PRIVATE)
                    val isProfileComplete = prefs.getBoolean("isProfileComplete", false)

                    if (isProfileComplete) {

                        startActivity(Intent(this, MainActivity::class.java))
                    } else {

                        startActivity(Intent(this, FormActivity::class.java))
                    }
                    finish()
                } else{
                    Toast.makeText(this,response.message,Toast.LENGTH_SHORT).show()
                }
            }

        }
    }
