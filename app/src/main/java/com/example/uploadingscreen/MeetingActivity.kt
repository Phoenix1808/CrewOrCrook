package com.example.uploadingscreen

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.uploadingscreen.databinding.ActivityMeetingActivityBinding
import com.example.uploadingscreen.network.SocketManager
import org.json.JSONObject

class MeetingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeetingActivityBinding

    private var roomCode: String? = null
    private val votedPlayer  = mutableSetOf<String>()

    private val playerMap = mutableMapOf<String, String>() // userId -> username

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMeetingActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomCode = intent.getStringExtra("roomCode")

        val userIds = intent.getStringArrayExtra("userIds")
        val usernames = intent.getStringArrayExtra("usernames")

        if (userIds != null && usernames != null) {
            for (i in userIds.indices) {
                playerMap[userIds[i]] = usernames[i]
            }
        }

        showPlayers()
        VoteUpdate()
        voteResult()
        freeplayResumed()

        binding.btnVote.setOnClickListener {

            if (playerMap.isNotEmpty()) {
                val firstPlayerId = playerMap.keys.first()
                sendVote(firstPlayerId)
            }
        }

        binding.btnSkip.setOnClickListener {
            sendVote(null)
        }

        binding.btnEndVoting.setOnClickListener {
            resolveVote()
        }
    }

    private fun showPlayers() {

//        val playersText = StringBuilder()
//
//        //new concept of destructuring declaration this means ignore the key but give me the values hence "_" means unused variable
//        for ((_, username) in playerMap) {
//            playersText.append(username).append("\n")
//        }
//
//        binding.tvPlayers.text = playersText.toString()
        updateList()
    }

    private fun sendVote(targetId: String?) {

        val socket = SocketManager.getSocket() ?: return

        val payload = JSONObject().apply {

            put("roomCode", roomCode)
            put("targetId", targetId)
        }

        socket.emit("game:vote", payload, io.socket.client.Ack { ackArgs ->

            if (ackArgs.isNotEmpty() && ackArgs[0] is JSONObject) {

                val ack = ackArgs[0] as JSONObject
                val ok = ack.optBoolean("ok")

                runOnUiThread {

                    if (ok) {

                        binding.btnVote.isEnabled = false
                        binding.btnSkip.isEnabled = false
                        binding.btnEndVoting.isEnabled = false
                        Log.d("VOTE_SENT", "Vote sent successfully")

                    } else {

                        val msg = ack.optString("message")

                        Log.d("VOTE_ERROR", msg)
                    }
                }
            }
        })
    }

    private fun VoteUpdate() {

        val socket = SocketManager.getSocket() ?: return

        socket.off("game:vote-update")

        socket.on("game:vote-update") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject

                val voterId = data.getString("voterId")

                val username = playerMap[voterId] ?: "Unknown"
                votedPlayer.add(voterId)

                Log.d("VOTE_UPDATE", "$username has voted")

                runOnUiThread {
                //                    binding.tvPlayers.text =
//                        binding.tvPlayers.text.toString() + "\n$username voted"
                    updateList()
                }
            }
        }
    }

    private fun resolveVote(){
        val socket = SocketManager.getSocket()?:return
        val payload = JSONObject().apply{
            put("roomCode",roomCode)
        }
        socket.emit("game:resolve-votes",payload,io.socket.client.Ack {ackArgs->
            if(ackArgs.isNotEmpty() && ackArgs[0] is JSONObject){
                val ack = ackArgs[0] as JSONObject
                val ok = ack.optBoolean("ok")

                runOnUiThread {
                    if(ok){
                        Log.d("RESOLVE_VOTES","Voting ended early by host")
                    } else{
                        Log.d("RESOLVE_VOTES","Failed to resolve votes")
                    }
                }
            }
        })
    }

    private fun voteResult(){
        val socket = SocketManager.getSocket()?:return

        socket.off("game:vote-result")
        socket.on("game:vote-result"){args->
            val data = args[0] as JSONObject
            val result = data.getJSONObject("result")
            val type = result.getString("type")

            runOnUiThread {
                if(type == "eject"){
                    val playerId = result.getString("playerId")
                    val username = playerMap[playerId] ?: "Unknown"

                    binding.tvPlayers.text = "$username was ejected"
                    Log.d("VOTE_RESULT","$username was ejected")
                } else{
                    binding.tvPlayers.text = "Vote tied. No one ejected"
                    Log.d("VOTE_RESULT","Vote tie")
                }
            }
        }
    }

    private fun freeplayResumed(){

        val socket = SocketManager.getSocket()?:return
        socket.off("game:freeplay-resumed")
        socket.on("game:freeplay-resumed"){
            Log.d("FREEPLAY","Returning to GameActivity")
            runOnUiThread { finish() }
        }
    }

    private fun updateList(){
        val text = StringBuilder()
        for((userId,username) in playerMap){
            if(votedPlayer.contains(userId)){
                text.append("$username voted \n")
            } else{
                text.append("$username \n")
            }
        }
        binding.tvPlayers.text = text.toString()
    }

    override fun onDestroy() {

        super.onDestroy()

        val socket = SocketManager.getSocket()

        socket?.off("game:vote-update")
        socket?.off("game:vote-result")
        socket?.off("game:freeplay-resumed")
    }
}
