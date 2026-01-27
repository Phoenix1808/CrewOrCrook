package com.example.uploadingscreen

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.Toast

class FormActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        val branch = findViewById<EditText>(R.id.etBranch)
        val year = findViewById<EditText>(R.id.etYear)
        val group = findViewById<EditText>(R.id.etGroup)
        val btncontinue = findViewById<Button>(R.id.btnContinue)

        btncontinue.setOnClickListener {
            val branch = branch.text.toString().trim()
            val year = year.text.toString().trim()
            val group = group.text.toString().trim()

            if(branch.isEmpty()|| year.isEmpty() || group.isEmpty()){
                Toast.makeText(
                    this,
                    "Please fill all details",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val prefs = getSharedPreferences("users_prefs",MODE_PRIVATE)
            prefs.edit()
                .putBoolean("isProfileComplete",true)
                .apply()

            startActivity(Intent(this,AvatarActivity::class.java))
            finish()
        }
    }
}