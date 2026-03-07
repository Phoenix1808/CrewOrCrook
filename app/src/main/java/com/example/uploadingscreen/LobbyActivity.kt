package com.example.uploadingscreen

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uploadingscreen.adapter.RoomAdapter
import com.example.uploadingscreen.model.Player
import com.example.uploadingscreen.network.SocketManager
import com.example.uploadingscreen.utils.Resource
import com.example.uploadingscreen.viewmodel.RoomViewModel
import io.socket.client.Ack
import org.json.JSONObject

class LobbyActivity : AppCompatActivity() {

    private lateinit var viewModel: RoomViewModel
    private var authToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        viewModel = ViewModelProvider(this)[RoomViewModel::class.java]

        authToken = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("token", null)

        if (authToken == null) {
            toast("Authentication token missing")
            return
        }

        SocketManager.init(authToken!!)
        SocketManager.connect()

        val rvRooms = findViewById<RecyclerView>(R.id.rvRooms)
        val btnCreateRoom = findViewById<Button>(R.id.btnCreateRoom)
        val btnFetchRoom = findViewById<Button>(R.id.btnFetchRoom)
        val btnLookupRoom = findViewById<Button>(R.id.btnLookupRoom)
        val etRoomCode = findViewById<EditText>(R.id.etRoomCode)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvCreateResult = findViewById<TextView>(R.id.tvCreateResult)
        val tvLookupResult = findViewById<TextView>(R.id.tvLookupResult)

        val adapter = RoomAdapter(emptyList()) { room ->
            viewModel.lookupRoom(authToken!!, room.code)
        }

        rvRooms.layoutManager = LinearLayoutManager(this)
        rvRooms.adapter = adapter

        btnCreateRoom.setOnClickListener {
            viewModel.createRoom(authToken!!)
        }

        btnFetchRoom.setOnClickListener {
            viewModel.getAvailableRooms(authToken!!)
        }

        btnLookupRoom.setOnClickListener {

            val code = etRoomCode.text.toString().trim()

            if (code.isEmpty()) {
                etRoomCode.error = "Enter room code"
                return@setOnClickListener
            }

            viewModel.lookupRoom(authToken!!, code)
        }

        viewModel.createRoom.observe(this) { resource ->

            when (resource) {

                is Resource.Loading ->
                    progressBar.visibility = ProgressBar.VISIBLE

                is Resource.Success -> {

                    progressBar.visibility = ProgressBar.GONE

                    val roomCode = resource.data?.code
                    tvCreateResult.text = "Created Room Code: $roomCode"

                    roomCode?.let {
                        waitAndJoinRoom(it, true)
                    }
                }

                is Resource.Error -> {
                    progressBar.visibility = ProgressBar.GONE
                    toast(resource.message)
                }
            }
        }

        viewModel.lookupRoom.observe(this) { resource ->

            when (resource) {

                is Resource.Loading ->
                    progressBar.visibility = ProgressBar.VISIBLE

                is Resource.Success -> {

                    progressBar.visibility = ProgressBar.GONE

                    val roomCode = resource.data?.code
                    val players = resource.data?.players

                    tvLookupResult.text = "Lookup Success: $roomCode"

                    roomCode?.let {
                        waitAndJoinRoom(it, false, players)
                    }
                }

                is Resource.Error -> {
                    progressBar.visibility = ProgressBar.GONE
                    toast(resource.message)
                }
            }
        }
    }

    private fun waitAndJoinRoom(
        roomCode: String,
        isHost: Boolean,
        players: List<Player>? = null
    ) {

        val socket = SocketManager.getSocket() ?: return

        if (socket.connected()) {

            joinRoom(roomCode, isHost, players)

        } else {

            socket.once(io.socket.client.Socket.EVENT_CONNECT) {

                runOnUiThread {
                    joinRoom(roomCode, isHost, players)
                }
            }
        }
    }

    private fun joinRoom(
        roomCode: String,
        isHost: Boolean,
        players: List<Player>? = null
    ) {

        val socket = SocketManager.getSocket() ?: return

        val payload = JSONObject().apply {
            put("roomCode", roomCode)
        }

        socket.emit("lobby:join-room", payload, Ack {

            val intent = Intent(this, WaitinRoomActivity::class.java)

            intent.putExtra("roomCode", roomCode)
            intent.putExtra("isHost", isHost)

            if (players != null) {

                val usernames = players.map { it.username }.toTypedArray()
                intent.putExtra("players", usernames)

            }

            startActivity(intent)
        })
    }

    private fun toast(msg: String?) {
        Toast.makeText(this, msg ?: "Something went wrong", Toast.LENGTH_SHORT).show()
    }
}