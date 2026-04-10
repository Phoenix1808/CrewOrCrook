package com.example.uploadingscreen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.uploadingscreen.databinding.ActivityLobbyBinding
import com.example.uploadingscreen.model.Player
import com.example.uploadingscreen.network.SocketManager
import com.example.uploadingscreen.utils.Resource
import com.example.uploadingscreen.viewmodel.RoomViewModel

class LobbyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLobbyBinding
    private lateinit var viewModel: RoomViewModel
    private var authToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLobbyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[RoomViewModel::class.java]

        authToken = intent.getStringExtra("token")

        Log.d("TOKEN_DEBUG", "Token: $authToken")

        if (authToken.isNullOrEmpty()) {
            toast("Authentication token missing")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        SocketManager.init(authToken!!)
        SocketManager.connect()

        binding.btnCreateRoom.setOnClickListener {
            viewModel.createRoom(authToken!!)
        }

        binding.btnLookupRoom.setOnClickListener {

            val code = binding.etRoomCode.text.toString().trim().uppercase()

            if (code.isEmpty()) {
                binding.etRoomCode.error = "Enter room code"
                return@setOnClickListener
            }

            viewModel.lookupRoom(authToken!!, code)
        }

        observeViewModel()
    }

    private fun observeViewModel() {

        viewModel.createRoom.observe(this) { resource ->

            when (resource) {

                is Resource.Loading ->
                    binding.progressBar.visibility = View.VISIBLE

                is Resource.Success -> {

                    binding.progressBar.visibility = View.GONE

                    val roomCode = resource.data?.code

                    roomCode?.let {
                        SocketManager.setCurrentRoom(it)
                        openWaitingRoom(it, true, null)
                    }
                }

                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    toast(resource.message)
                }
            }
        }

        viewModel.lookupRoom.observe(this) { resource ->

            when (resource) {

                is Resource.Loading ->
                    binding.progressBar.visibility = View.VISIBLE

                is Resource.Success -> {

                    binding.progressBar.visibility = View.GONE

                    val roomCode = resource.data?.code
                    val players = resource.data?.players

                    roomCode?.let {
                        SocketManager.setCurrentRoom(it)
                        openWaitingRoom(it, false, players)
                    }
                }

                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    toast(resource.message)
                }
            }
        }
    }

    private fun openWaitingRoom(
        roomCode: String,
        isHost: Boolean,
        players: List<Player>?
    ) {

        val intent = Intent(this, WaitinRoomActivity::class.java)

        intent.putExtra("roomCode", roomCode)
        intent.putExtra("isHost", isHost)

        players?.let {
            val usernames = it.map { player -> player.username }.toTypedArray()
            intent.putExtra("players", usernames)
        }

        startActivity(intent)
    }

    private fun toast(msg: String?) {
        Toast.makeText(this, msg ?: "Something went wrong", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
//        SocketManager.clrRoom()
    }
}