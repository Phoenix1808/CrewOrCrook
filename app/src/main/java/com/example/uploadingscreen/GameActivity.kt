package com.example.uploadingscreen

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uploadingscreen.network.SocketManager
import org.json.JSONObject

class GameActivity : AppCompatActivity() {

    private var roomCode: String? = null
    private var role: String? = null

    private lateinit var tvRoomCode: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvStatus : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)

        roomCode = intent.getStringExtra("roomCode")

        tvRole=findViewById<TextView>(R.id.tvRole)
        tvRoomCode= findViewById<TextView>(R.id.tvRoomCode)
        tvStatus = findViewById<TextView>(R.id.tvStatus)

        tvRoomCode.text = "Room Code : $roomCode"
        tvStatus.text = "Waiting For Role.."

        setupGameSocketListener()
    }

    private fun setupGameSocketListener(){
        val socket= SocketManager.getSocket() ?: return
        socket.on("game:role"){args->
            runOnUiThread {
                if(args.isNotEmpty() && args[0] is JSONObject){
                    val data = args[0] as JSONObject
                    role = data.optString("role")

                    tvRole.text = "Role: $role"
                    tvStatus.text = "Game Started BROOOO..."

                    if(role=="imposter"){
                        tvRole.setTextColor(getColor(android.R.color.holo_red_dark))
                    } else{
                        tvRole.setTextColor(getColor(android.R.color.holo_green_dark))
                    }

                }
            }
        }
    }
}