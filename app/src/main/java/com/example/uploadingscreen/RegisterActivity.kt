package com.example.uploadingscreen

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.content.Intent
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.uploadingscreen.model.SignUpRequest
import com.example.uploadingscreen.viewmodel.AuthViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val username = findViewById<EditText>(R.id.etUserName)
        val zealId = findViewById<EditText>(R.id.etZealID)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnconfirm = findViewById<ImageView>(R.id.btnconfirm)

        btnconfirm.setOnClickListener {
            val username = username.text.toString().trim()
            val zealId = zealId.text.toString().trim()
            val password = password.text.toString().trim()

            if (username.isEmpty() || zealId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All Fields Required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val req = SignUpRequest(username, zealId, password)
            viewModel.register(req)
        }

        viewModel.signUpRes.observe(this) { response ->
            if (response.success) {
                Toast.makeText(this, "SignUp Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, FormActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "SignUp Failed", Toast.LENGTH_SHORT).show()

            }
        }
    }
}