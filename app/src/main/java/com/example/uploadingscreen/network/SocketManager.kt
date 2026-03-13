package com.example.uploadingscreen.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import com.example.uploadingscreen.utils.Constant.BASE_URL
import org.json.JSONObject

object SocketManager {

    private var socket: Socket? = null
    private var currentRoomCode: String? = null

    fun init(token: String) {
        try {

            if (socket != null) {
                Log.d("SOCKET", "Socket already initialized")
                return
            }

            val opts = IO.Options()
            opts.auth = mapOf("token" to token)
            opts.reconnection = true
            opts.reconnectionAttempts = Int.MAX_VALUE
            opts.reconnectionDelay = 2000

            socket = IO.socket(BASE_URL, opts)

        } catch (e: Exception) {
            Log.e("SOCKET", "Initialization error: ${e.message}")
        }
    }

    fun connect() {

        val s = socket ?: return

        if (s.connected()) {
            Log.d("SOCKET", "Already connected")
            return
        }

        s.on(Socket.EVENT_CONNECT) {

            Log.d("SOCKET", "Connected ID: ${s.id()}")

            if (currentRoomCode != null) {
                rejoinRoom()
            }
        }

        s.on(Socket.EVENT_DISCONNECT) {
            Log.d("SOCKET", "Disconnected")
        }

        s.on(Socket.EVENT_CONNECT_ERROR) {
            Log.e("SOCKET", "Connection error")
        }

        s.connect()
    }

    fun setCurrentRoom(roomCode: String) {
        currentRoomCode = roomCode
    }

    fun clrRoom() {
        currentRoomCode = null
    }

    private fun rejoinRoom() {

        val s = socket ?: return
        val room = currentRoomCode ?: return

        val payload = JSONObject().apply {
            put("roomCode", room)
        }

        s.emit("lobby:join-room", payload)

        Log.d("SOCKET", "Rejoined room after reconnect")
    }

    fun getSocket(): Socket? = socket

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }
}