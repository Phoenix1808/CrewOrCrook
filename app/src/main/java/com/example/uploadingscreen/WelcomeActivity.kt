package com.example.uploadingscreen

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

//        val token = getSharedPreferences("auth", MODE_PRIVATE)
//            .getString("token", null)
//
//        // If already logged in
//        if (token != null) {
//
//            startActivity(Intent(this, LobbyActivity::class.java))
//            finish()
//            return
//        }

        // Signup button
        findViewById<View>(R.id.btnSignup).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Login button
        findViewById<View>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}