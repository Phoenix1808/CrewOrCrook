package com.example.uploadingscreen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etZeal = findViewById<EditText>(R.id.etZealId)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {

            val zeal = etZeal.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (zeal.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val prefs = getSharedPreferences("users_prefs", MODE_PRIVATE)
            val isProfileComplete = prefs.getBoolean("isProfileComplete", false)

            if (isProfileComplete) {

                startActivity(Intent(this, MainActivity::class.java))
            } else {

                startActivity(Intent(this, FormActivity::class.java))
            }

            finish()
        }
    }
}
