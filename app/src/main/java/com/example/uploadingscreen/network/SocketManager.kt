package com.example.uploadingscreen.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket

object SocketManager {

    private const val BASE_URL = "https://creworcrook.onrender.com/"

    private var socket: Socket? = null

    fun init(token: String) {
        try {
            val opts = IO.Options()
            opts.auth = mapOf("token" to token)

            socket = IO.socket(BASE_URL, opts)
        } catch (e: Exception) {
            Log.e("SOCKET", "Initialization error: ${e.message}")
        }
    }

    fun connect() {
        socket?.connect()

        socket?.on(Socket.EVENT_CONNECT) {
            Log.d("SOCKET", "Connected successfully !!")
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.d("SOCKET", "Disconnected !!")
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d("SOCKET", "Connection Error ❗")
        }
    }

    fun getSocket(): Socket? = socket

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
    }
}
