package com.example.uploadingscreen

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uploadingscreen.adapter.PlayerAdapter
import com.example.uploadingscreen.databinding.ActivityWaitinRoomBinding
import com.example.uploadingscreen.network.SocketManager
import org.json.JSONObject

class WaitinRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaitinRoomBinding

    private lateinit var adapter: PlayerAdapter
    private val players = mutableListOf<String>()
    private val playerMap = HashMap<String,String>()

    private var roomCode: String? = null
    private var isHost: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityWaitinRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomCode = intent.getStringExtra("roomCode")
        isHost = intent.getBooleanExtra("isHost", false)

        binding.tvRoomCode.text = "Room Code: $roomCode"

        adapter = PlayerAdapter(players)

        binding.rvPlayers.layoutManager = LinearLayoutManager(this)
        binding.rvPlayers.adapter = adapter

        binding.btnStartGame.isEnabled = isHost

        binding.btnStartGame.setOnClickListener {

            val socket = SocketManager.getSocket() ?: return@setOnClickListener

            val payload = JSONObject().apply {
                put("roomCode", roomCode)
            }

            android.util.Log.d("GAME_DEBUG", "Host clicked start")

            socket.emit("game:start", payload, io.socket.client.Ack { args ->
                android.util.Log.d("GAME_DEBUG", "Start game ACK: $args")
            })
        }

        val existingPlayers = intent.getStringArrayExtra("players")
        existingPlayers?.let {

            players.clear()
            players.addAll(it)

            if (isHost && players.isNotEmpty()) {
                players[0] = players[0] + " (Host)"
            }

            adapter.notifyDataSetChanged()
        }

        joinRoomSocket()
        setupSocket()
    }

    private fun setupSocket() {

        val socket = SocketManager.getSocket() ?: return

        socket.off("lobby:players-list")
        socket.off("game:started")
        socket.off("game:role")
        socket.off("lobby:player-joined")

        socket.on("lobby:players-list") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject
                val playersArray = data.getJSONArray("players")
                val hostId = data.getString("hostId")

                runOnUiThread {

                    players.clear()
                    playerMap.clear()

                    for (i in 0 until playersArray.length()) {

                        val player = playersArray.getJSONObject(i)
                        val username = player.getString("username")
                        val userId = player.getString("userId")

                        playerMap[userId] = username
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

        socket.on("lobby:player-joined") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject
                val username = data.getString("username")

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "$username joined the room",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
        }

        socket.on("game:started") {
               android.util.Log.d("GAME_DEBUG","game:started recieved")
            runOnUiThread {
                // optional loading UI
            }
        }

        socket.on("game:role") { args ->

            android.util.Log.d("GAME_DEBUG","game:role received :$args")
            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject
                val role = data.optString("role")

                runOnUiThread {

                    val intent = Intent(this, GameActivity::class.java)

                    intent.putExtra("roomCode", roomCode)
                    intent.putExtra("role", role)

                    val userIds = playerMap.keys.toTypedArray()
                    val usernames = playerMap.values.toTypedArray()
                    intent.putExtra("userIds",userIds)
                    intent.putExtra("usernames",usernames)

                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun joinRoomSocket() {

        val socket = SocketManager.getSocket() ?: return

        val payload = JSONObject().apply {
            put("roomCode", roomCode)
        }
        //this enables the auto-rejoin after reconnection
        SocketManager.setCurrentRoom(roomCode!!)

        socket.emit("lobby:join-room", payload)
    }

    override fun onDestroy() {
        super.onDestroy()

        val socket = SocketManager.getSocket()

        socket?.off("lobby:players-list")
        socket?.off("lobby:player-joined")
        socket?.off("game:started")
        socket?.off("game:role")
    }
}