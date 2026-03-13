package com.example.uploadingscreen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.uploadingscreen.databinding.ActivityGameBinding
import com.example.uploadingscreen.network.SocketManager
import com.example.uploadingscreen.socket.TaskHandler
import com.google.android.gms.location.*
import org.json.JSONObject
import com.example.uploadingscreen.network.GameEndHandler
data class DeadBody(
    val victimId: String,
    val lat: Double,
    val lng: Double
)

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding

    private val playerMap = mutableMapOf<String, String>()

    private var roomCode: String? = null
    private var role: String? = null

    private var currentVictimId: String? = null


    private val deadBodies = mutableListOf<DeadBody>()
    private var reportTargetBody: DeadBody? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback


    //socket events for tasks:progress & task:complete
    private lateinit var taskHandler : TaskHandler

    private lateinit var lifeHandler : GameEndHandler

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 101
        private const val REPORT_RANGE_METRES = 8f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userIds = intent.getStringArrayExtra("userIds")
        val usernames = intent.getStringArrayExtra("usernames")

        if (userIds != null && usernames != null) {
            for (i in userIds.indices) {
                playerMap[userIds[i]] = usernames[i]
            }
        }

        //room
        roomCode = intent.getStringExtra("roomCode")
        role = intent.getStringExtra("role")

        binding.tvRoomCode.text = "Room Code : $roomCode"

        if (role != null) {

            binding.tvRole.text = "Role: $role"
            binding.tvStatus.text = "Game Started"

            if (role == "imposter") {
                binding.tvRole.setTextColor(getColor(android.R.color.holo_red_dark))
            } else {
                binding.tvRole.setTextColor(getColor(android.R.color.holo_green_dark))
                binding.btnKill.visibility = View.GONE
            }

        } else {
            binding.tvStatus.text = "Role Not Assigned"
        }

        //kill btn
        binding.btnKill.setOnClickListener {
            val victim = currentVictimId ?: return@setOnClickListener
            sendKill(victim)
        }

        //body report
        binding.btnReport.setOnClickListener {
            sendReportBody()
        }

        binding.btnReport.visibility = View.GONE

        //task:progress & task:complete
        taskHandler = TaskHandler(roomCode){completed,total ->
            runOnUiThread {
                binding.tvTaskProgress.text = "Tasks: $completed / $total"
            }
        }
        taskHandler.TaskProgress()

        binding.btnTask.setOnClickListener{
            if(role == "crewmate"){
                taskHandler.TaskComplete()
            }
        }

        //game:ended & game:error
        lifeHandler = GameEndHandler(this,role)
        lifeHandler.gameEnd()
        lifeHandler.gameError()

        listenForPlayerMovement()
        listenForTargets()
        listenForKillEvent()
        listenMeetingStart()
        requestBodies()

        setupLocation()
    }

    // fxn for location system
    private fun setupLocation() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation ?: return

                val lat = location.latitude
                val lng = location.longitude

                sendMove(lat, lng)

                binding.tvMyPosition.text = "My Position: $lat , $lng"

                checkBodyNearby(lat, lng)

                Log.d("REAL_GPS", "$lat,$lng")
            }
        }

        requestLocationPermission()
    }

    private fun requestLocationPermission() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )

        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000
        )
            .setMinUpdateIntervalMillis(1000)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                mainLooper
            )
        }
    }

    // movement fxn
    private fun sendMove(lat: Double, lng: Double) {

        val socket = SocketManager.getSocket() ?: return

        val payload = JSONObject().apply {

            put("roomCode", roomCode)

            put(
                "position",
                JSONObject().apply {
                    put("lat", lat)
                    put("lng", lng)
                }
            )
        }

        socket.emit("game:move", payload)

        Log.d("MOVE_SENT", "My position: $lat,$lng")
    }

    // movement listener
    private fun listenForPlayerMovement() {

        val socket = SocketManager.getSocket() ?: return

        socket.off("game:player-moved")

        socket.on("game:player-moved") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject

                val userId = data.getString("userId")
                val username = playerMap[userId] ?: "Unknown"

                val position = data.getJSONObject("position")

                val lat = position.getDouble("lat")
                val lng = position.getDouble("lng")

                runOnUiThread {

                    binding.tvLastMovement.text =
                        "$username moved to $lat , $lng"

                    Log.d("PLAYER_MOVED", "$username moved to $lat,$lng")
                }
            }
        }
    }

    // nearby:targets sockets implementation
    private fun listenForTargets() {

        val socket = SocketManager.getSocket() ?: return

        socket.off("game:nearby-targets")

        socket.on("game:nearby-targets") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject
                val targets = data.getJSONArray("targets")

                runOnUiThread {

                    val count = targets.length()

                    binding.tvNearbyPlayers.text = "Nearby Players: $count"

                    binding.btnKill.isEnabled =
                        (role == "imposter" && count > 0)

                    binding.btnKill.alpha =
                        if (count > 0) 1f else 0.5f

                    if (count > 0) {

                        val victim = targets.getJSONObject(0)

                        currentVictimId = victim.getString("userId")

                        Log.d("TARGETS", "Player nearby: $currentVictimId")

                    } else {

                        currentVictimId = null
                    }
                }
            }
        }
    }

    // kill_sent sockets
    private fun sendKill(victimId: String) {

        val socket = SocketManager.getSocket() ?: return

        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("victimId", victimId)
        }

        socket.emit("game:kill", payload)

        binding.btnKill.isEnabled = false

        Log.d("KILL_SENT", "Kill sent for victim: $victimId")
    }

    // kill_event listener
    private fun listenForKillEvent() {

        val socket = SocketManager.getSocket() ?: return

        socket.off("game:kill-event")

        socket.on("game:kill-event") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject

                val killerId = data.getString("killerId")
                val victimId = data.getString("victimId")

                val position = data.getJSONObject("position")

                val lat = position.getDouble("lat")
                val lng = position.getDouble("lng")

                deadBodies.add(DeadBody(victimId, lat, lng))

                val victimName = playerMap[victimId] ?: "Unknown"
                val killerName = playerMap[killerId] ?: "Unknown"

                runOnUiThread {

                    binding.tvStatus.text =
                        "$killerName killed $victimName !!"

                    Log.d("BODY_CREATED", "Body of $victimName at $lat,$lng")
                }
            }
        }
    }

    // report body socket event
    private fun sendReportBody() {

        val victimId = reportTargetBody?.victimId ?: return
        val socket = SocketManager.getSocket() ?: return

        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("bodyVictimId", victimId)
        }

        socket.emit("game:report-body", payload, io.socket.client.Ack { ackArgs ->

            if (ackArgs.isNotEmpty() && ackArgs[0] is JSONObject) {

                val ack = ackArgs[0] as JSONObject
                val ok = ack.optBoolean("ok")

                runOnUiThread {

                    if (ok) {

                        deadBodies.removeIf { it.victimId == victimId }

                        binding.tvStatus.text = "Body Reported!"
                        binding.btnReport.visibility = View.GONE

                        Log.d("REPORT_BODY", "Report successful")

                    } else {

                        val message = ack.optString("message")

                        Log.d("REPORT_BODY_ERROR", message)
                    }
                }
            }
        })
    }

    // body_Range
    private fun checkBodyNearby(myLat: Double, myLng: Double) {

        var foundBody: DeadBody? = null

        for (body in deadBodies) {

            val results = FloatArray(1)

            Location.distanceBetween(
                myLat,
                myLng,
                body.lat,
                body.lng,
                results
            )

            if (results[0] <= REPORT_RANGE_METRES) {
                foundBody = body
                break
            }
        }

        if (foundBody != null) {

            reportTargetBody = foundBody
            binding.btnReport.visibility = View.VISIBLE

            Log.d("BODY_RANGE", "Body in range: ${foundBody.victimId}")

        } else {

            reportTargetBody = null
            binding.btnReport.visibility = View.GONE
        }
    }

    // get:bodies sockets
    private fun requestBodies() {

        val socket = SocketManager.getSocket() ?: return

        val payload = JSONObject().apply {
            put("roomCode", roomCode)
        }

        socket.emit("game:get-bodies", payload, io.socket.client.Ack { ackArgs ->

            if (ackArgs.isNotEmpty() && ackArgs[0] is JSONObject) {

                val data = ackArgs[0] as JSONObject
                val ok = data.optBoolean("ok")

                if (!ok) {
                    Log.d("GET_BODIES_ERROR", data.optString("message"))
                    return@Ack
                }

                val bodies = data.getJSONArray("bodies")

                deadBodies.clear()

                for (i in 0 until bodies.length()) {

                    val body = bodies.getJSONObject(i)

                    deadBodies.add(
                        DeadBody(
                            body.getString("victimId"),
                            body.getDouble("lat"),
                            body.getDouble("lng")
                        )
                    )
                }

                Log.d("GET_BODIES", "Bodies restored: ${deadBodies.size}")
            }
        })
    }

     // meeting:started sockets
    private fun listenMeetingStart() {

        val socket = SocketManager.getSocket() ?: return

        socket.off("game:meeting-started")

        socket.on("game:meeting-started") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject

                val reason = data.optString("reason")
                val reporterId = data.optString("reporterId")
                val victimId = data.optString("bodyVictimId")

                Log.d("MEETING_STARTED", "Meeting triggered: $reason")

                runOnUiThread {

                    fusedLocationClient.removeLocationUpdates(locationCallback)

                    val intent = Intent(this, MeetingActivity::class.java)

                    intent.putExtra("roomCode", roomCode)
                    intent.putExtra("reason", reason)
                    intent.putExtra("reporterId", reporterId)
                    intent.putExtra("victimId", victimId)

                    startActivity(intent)
                }
            }
        }
    }


    //when meeting finishes then android returns to existing Game instance but inside this activity
    // we stopped location updates when meeting started
    // so after returning gps,movememt,nearby detection and kill detection may stop
    //hence we used this onResume

    override fun onResume(){
        super.onResume()
        if(::fusedLocationClient.isInitialized){
            Log.d("GAME_RESUME","restarting location updates")
            startLocationUpdates()
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        val socket = SocketManager.getSocket()

        socket?.off("game:player-moved")
        socket?.off("game:nearby-targets")
        socket?.off("game:kill-event")
        socket?.off("game:meeting-started")
        taskHandler.cleanup()
        lifeHandler.cleanup()

        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}