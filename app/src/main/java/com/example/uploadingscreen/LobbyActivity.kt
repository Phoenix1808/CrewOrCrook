package com.example.uploadingscreen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uploadingscreen.adapter.RoomAdapter
import com.example.uploadingscreen.network.SocketManager
import com.example.uploadingscreen.utils.Resource
import com.example.uploadingscreen.viewmodel.RoomViewModel
import io.socket.client.Ack
import org.json.JSONObject

//after emitting the event of game:start the users are getting connected and only host is permitted to start the game but the issue is there is a toast coming that u need at least 3 more players to begin and hence UI ain't navigating to GameActivity.
class LobbyActivity : AppCompatActivity() {

    private lateinit var viewModel: RoomViewModel
    private var authToken: String? = null
    private var joinedRoomCode: String? = null
    private lateinit var btnStartGame: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        viewModel = ViewModelProvider(this)[RoomViewModel::class.java]

        authToken = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("token", null)

        Log.d("SOCKET_DEBUG", "Token: $authToken")

        if (authToken == null) {
            toast("Authentication token missing")
            return
        }

        //  initialize & connect socket
        SocketManager.init(authToken!!)
        SocketManager.connect()

        setupSocketListeners()


        val rvRooms = findViewById<RecyclerView>(R.id.rvRooms)
        val btnCreateRoom = findViewById<Button>(R.id.btnCreateRoom)
        val btnFetchRoom = findViewById<Button>(R.id.btnFetchRoom)
        val btnLookupRoom = findViewById<Button>(R.id.btnLookupRoom)
        val etRoomCode = findViewById<EditText>(R.id.etRoomCode)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvCreateResult = findViewById<TextView>(R.id.tvCreateResult)
        val tvLookupResult = findViewById<TextView>(R.id.tvLookupResult)
        btnStartGame = findViewById<Button>(R.id.btnStartGame)

        btnStartGame.isEnabled= false

        btnStartGame.setOnClickListener {
            startGame()
        }


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
                is Resource.Loading -> progressBar.visibility = View.VISIBLE

                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    val roomCode = resource.data?.code
                    tvCreateResult.text = "Created Room Code: $roomCode"

                    roomCode?.let {
                        waitAndJoinRoom(it)
                    }
                }

                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    toast(resource.message)
                }
            }
        }

        viewModel.lookupRoom.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> progressBar.visibility = View.VISIBLE

                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    val roomCode = resource.data?.code
                    tvLookupResult.text = "Lookup Success: $roomCode"

                    roomCode?.let {
                        waitAndJoinRoom(it)
                    }
                }

                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    toast(resource.message)
                }
            }
        }

        viewModel.availableRooms.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> progressBar.visibility = View.VISIBLE

                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    resource.data?.let { adapter.updateData(it) }
                }

                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    toast(resource.message)
                }
            }
        }
    }

    //  wait until socket connected before joining
    private fun waitAndJoinRoom(roomCode: String) {

        val socket = SocketManager.getSocket() ?: return

        if (socket.connected()) {
            joinRoom(roomCode)
        } else {
            toast("Waiting for socket connection...")
            socket.once(io.socket.client.Socket.EVENT_CONNECT) {
                runOnUiThread {
                    joinRoom(roomCode)
                }
            }
        }
    }
//game initialization k liye
    private fun joinRoom(roomCode: String) {

        val socket = SocketManager.getSocket() ?: return

        val payload = JSONObject().apply {
            put("roomCode", roomCode)
        }

        Log.d("SOCKET_DEBUG", "Emitting lobby:join-room")

        socket.emit("lobby:join-room", payload, Ack { args ->

            runOnUiThread {

                Log.d("SOCKET_DEBUG", "ACK raw: ${args.contentToString()}")

                if (args.isEmpty()) {
                    toast("No ACK received")
                    return@runOnUiThread
                }

                val firstArg = args[0]

                when (firstArg) {

                    is JSONObject -> {
                        val ok = firstArg.optBoolean("ok", false)
                        val message = firstArg.optString("message", "")

                        if (ok) {
                            joinedRoomCode = roomCode
                            btnStartGame.isEnabled = true
                            toast("Joined successfully")
                        } else {
                            toast("Join failed: $message")
                        }
                    }

                    is Boolean -> {
                        if (firstArg) {
                            joinedRoomCode = roomCode
                            btnStartGame.isEnabled = true
                            toast("Joined successfully")
                        } else {
                            toast("Join failed")
                        }
                    }

                    is String -> {
                        toast(firstArg)
                    }

                    else -> {
                        toast("Unknown ACK type: ${firstArg::class.java.simpleName}")
                    }
                }
            }
        })
    }

    private fun setupSocketListeners() {

        val socket = SocketManager.getSocket() ?: return

        socket.off()

        socket.on("lobby:player-joined") { args ->
            runOnUiThread {
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val userId = data.optString("user")
                    toast("Player joined: $userId")
                }
            }
        }

        socket.on("game:started") {
            runOnUiThread {
                Log.d("SOCKET_DEBUG", "game:started event received")
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("roomCode",joinedRoomCode)
                startActivity(intent)
            }
        }


        socket.on("game:error") { args ->
            runOnUiThread {
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val message = data.optString("message")
                    toast("Game Error: $message")
                }
            }
        }
    }


    private fun toast(msg: String?) {
        Toast.makeText(this, msg ?: "Something went wrong", Toast.LENGTH_SHORT).show()
    }

    private fun startGame() {

        val socket = SocketManager.getSocket() ?: return
        val roomCode = joinedRoomCode ?: return

        val payload = JSONObject().apply {
            put("roomCode", roomCode)
        }

        Log.d("SOCKET_DEBUG", "Emitting game:start")

        socket.emit("game:start", payload, Ack { args ->

            runOnUiThread {

                Log.d("SOCKET_DEBUG", "StartGame ACK raw: ${args.contentToString()}")

                if (args.isNotEmpty() && args[0] is JSONObject) {

                    val response = args[0] as JSONObject
                    val ok = response.optBoolean("ok", false)

                    if (ok) {

                        Log.d("SOCKET_DEBUG", "Start game acknowledged OK")


                        val intent = Intent(this, GameActivity::class.java)
                        intent.putExtra("roomCode", joinedRoomCode)
                        startActivity(intent)

                    } else {
                        toast(response.optString("message"))
                    }
                }
            }
        })
    }


}
