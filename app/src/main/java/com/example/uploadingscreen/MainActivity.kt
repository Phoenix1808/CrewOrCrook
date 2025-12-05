package com.example.uploadingscreen

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.content.Intent

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.uploadbtn).setOnClickListener {
            startActivity(Intent(this, UploadingActivity::class.java))
        }

        findViewById<Button>(R.id.btnSeq).setOnClickListener {
            startActivity(Intent(this, NumSeqActivity::class.java))
        }

        findViewById<Button>(R.id.btnMemory).setOnClickListener {
            startActivity(Intent(this, MemoryGameActivity::class.java))
        }

        findViewById<Button>(R.id.btnTrivia).setOnClickListener {
            startActivity(Intent(this, TriviaQuizActivity::class.java))
        }

        findViewById<Button>(R.id.ConnectDot).setOnClickListener {
            startActivity(Intent(this, ConnectDotActivity::class.java))
        }

    }
}
