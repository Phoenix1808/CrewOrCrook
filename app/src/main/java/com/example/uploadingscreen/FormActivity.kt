package com.example.uploadingscreen

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
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

        val branch = findViewById<AutoCompleteTextView>(R.id.etBranch)
        val year = findViewById<AutoCompleteTextView>(R.id.etYear)
        val group = findViewById<EditText>(R.id.etGroup)
        val btncontinue = findViewById<ImageView>(R.id.btnconfirm)

        //dropdown menu for branches in form activity
        val branchAdapter = ArrayAdapter(this,
         R.layout.dropdown_item,
            resources.getStringArray(R.array.branches)
        )

        //dropdown menu for years in form activity
        val yearAdapter = ArrayAdapter(this,
            R.layout.dropdown_item,
            resources.getStringArray(R.array.years)
        )
        branch.setAdapter(branchAdapter)
        year.setAdapter(yearAdapter)

        branch.setOnClickListener {
            branch.showDropDown()
        }
        year.setOnClickListener {
            year.showDropDown()
        }

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
