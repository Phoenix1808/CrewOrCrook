package com.example.uploadingscreen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uploadingscreen.adapter.PlayerAdapter
import com.example.uploadingscreen.network.SocketManager
import org.json.JSONObject

class WaitinRoomActivity : AppCompatActivity() {

    private lateinit var adapter: PlayerAdapter
    private val players = mutableListOf<String>()

    private var roomCode: String? = null
    private var isHost: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waitin_room)

        roomCode = intent.getStringExtra("roomCode")
        isHost = intent.getBooleanExtra("isHost", false)

        val tvRoomCode = findViewById<TextView>(R.id.tvRoomCode)
        val rvPlayers = findViewById<RecyclerView>(R.id.rvPlayers)
        val btnStartGame = findViewById<Button>(R.id.btnStartGame)

        tvRoomCode.text = "Room Code: $roomCode"

        adapter = PlayerAdapter(players)

        rvPlayers.layoutManager = LinearLayoutManager(this)
        rvPlayers.adapter = adapter

        btnStartGame.isEnabled = isHost

        btnStartGame.setOnClickListener {

            val socket = SocketManager.getSocket() ?: return@setOnClickListener

            val payload = JSONObject().apply {
                put("roomCode", roomCode)
            }

            socket.emit("game:start", payload)
        }

        setupSocket()
    }

    private fun setupSocket() {

        val socket = SocketManager.getSocket() ?: return

        socket.off("lobby:players-list")
        socket.off("game:started")
        socket.off("game:role")

        socket.on("lobby:players-list") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject
                val playersArray = data.getJSONArray("players")
                val hostId = data.getString("hostId")

                runOnUiThread {

                    players.clear()

                    for (i in 0 until playersArray.length()) {

                        val player = playersArray.getJSONObject(i)
                        val username = player.getString("username")
                        val userId = player.getString("userId")

                        if (userId == hostId) {
                            players.add("$username (Host)")
                        } else {
                            players.add(username)
                        }
                    }

                    adapter.notifyDataSetChanged()
                }
            }
        }

        socket.on("game:started") {

            runOnUiThread {

                // Optional loading screen
            }
        }

        socket.on("game:role") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject
                val role = data.optString("role")

                runOnUiThread {

                    val intent = Intent(this, GameActivity::class.java)

                    intent.putExtra("roomCode", roomCode)
                    intent.putExtra("role", role)

                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        val socket = SocketManager.getSocket()

        socket?.off("lobby:players-list")
        socket?.off("game:started")
        socket?.off("game:role")
    }
}