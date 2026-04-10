package com.example.uploadingscreen.socket

import android.util.Log
import com.example.uploadingscreen.network.SocketManager
import org.json.JSONObject

class TaskHandler(
    private val roomCode: String?,
    private val progressUpdate: (Int, Int) -> Unit
) {

    private val socket = SocketManager.getSocket()

    //gmae:task-complete
    fun TaskComplete() {

        val payload = JSONObject().apply {
            put("roomCode", roomCode)
        }

        socket?.emit("game:task-complete", payload, io.socket.client.Ack { ackArgs ->

            if (ackArgs.isNotEmpty() && ackArgs[0] is JSONObject) {

                val ack = ackArgs[0] as JSONObject
                val ok = ack.optBoolean("ok")

                if (ok) {
                    Log.d("TASK_COMPLETE", "Task completed")
                } else {
                    Log.d("TASK_ERROR", ack.optString("message"))
                }
            }
        })
    }

    //game:task-progress
    fun TaskProgress() {

        socket?.off("game:task-progress")

        socket?.on("game:task-progress") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject

                val completed = data.getInt("completed")
                val total = data.getInt("total")

                progressUpdate.invoke(completed, total)

                Log.d("TASK_PROGRESS", "$completed / $total")
            }
        }
    }

    fun cleanup() {
        socket?.off("game:task-progress")
    }
}