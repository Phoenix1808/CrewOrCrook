package com.example.uploadingscreen

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.content.Intent

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        findViewById<View>(R.id.btnconfirm).setOnClickListener {
            startActivity(Intent(this, FormActivity::class.java))
            finish()
        }
    }
}