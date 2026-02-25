package com.example.uploadingscreen

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.uploadingscreen.model.LoginRequest
import com.example.uploadingscreen.viewmodel.AuthViewModel
import android.widget.FrameLayout
import android.text.InputType

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel : AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val etusername = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<ImageView>(R.id.btnconfirm)
        val loader = findViewById<FrameLayout>(R.id.loaderLogin)
        val toggle = findViewById<ImageView>(R.id.toggle)

        viewModel.loading.observe(this){loading->
            loader.visibility = if(loading) View.VISIBLE else View.GONE
            btnLogin.isEnabled = !loading
        }

        var isVisible = false
        toggle.setOnClickListener {
            if(isVisible){
                etPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggle.setImageResource(R.drawable.ic_eye)
            } else{
                etPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggle.setImageResource(R.drawable.ic_eye_off)
            }
            etPass.setSelection(etPass.text.length)
            isVisible = !isVisible
        }

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
