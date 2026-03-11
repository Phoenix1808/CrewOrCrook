package com.example.uploadingscreen

import android.Manifest
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
import com.google.android.gms.location.*
import org.json.JSONObject

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding

    private val playerMap = mutableMapOf<String, String>()

    private var roomCode: String? = null
    private var role: String? = null

    private var currentVictimId: String? = null

    private var deadBodyId: String? = null
    private var deadBodyLat: Double? = null
    private var deadBodyLng: Double? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

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

        binding.btnKill.setOnClickListener {
            val victim = currentVictimId ?: return@setOnClickListener
            sendKill(victim)
        }

        binding.btnReport.setOnClickListener {
            sendReportBody()
        }

        binding.btnReport.visibility = View.GONE

        listenForPlayerMovement()
        listenForTargets()
        listenForKillEvent()
        requestBodies()
        setupLocation()
    }


//location
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

    //sends the movement
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

    //player-movement
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

    //NearBy-Targets
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

    //kill system
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


    //for kill-event
    private fun listenForKillEvent() {

        val socket = SocketManager.getSocket() ?: return

        socket.off("game:kill-event")

        socket.on("game:kill-event") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject

                val killerId = data.getString("killerId")
                val victimId = data.getString("victimId")

                val position = data.getJSONObject("position")

                deadBodyId = victimId
                deadBodyLat = position.getDouble("lat")
                deadBodyLng = position.getDouble("lng")

                val victimName = playerMap[victimId] ?: "Unknown"
                val killerName = playerMap[killerId] ?: "Unknown"

                runOnUiThread {

                    binding.tvStatus.text =
                        "$killerName killed $victimName !!"

                    Log.d(
                        "BODY_CREATED",
                        "Body of $victimName at $deadBodyLat,$deadBodyLng"
                    )
                }
            }
        }
    }


//to report the body
    private fun sendReportBody() {

        val victimId = deadBodyId ?: return
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

    //checks for range of bodies
    private fun checkBodyNearby(myLat: Double, myLng: Double) {

        if (deadBodyLat == null || deadBodyLng == null) {
            binding.btnReport.visibility = View.GONE
            return
        }

        val results = FloatArray(1)

        Location.distanceBetween(
            myLat,
            myLng,
            deadBodyLat!!,
            deadBodyLng!!,
            results
        )

        val distance = results[0]

        if (distance <= REPORT_RANGE_METRES) {

            binding.btnReport.visibility = View.VISIBLE

            Log.d("BODY_RANGE", "Body within report range → $distance m")

        } else {

            binding.btnReport.visibility = View.GONE
        }
    }

    //socket connection of get-bodies
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

                Log.d("GET_BODIES", "Bodies received: ${bodies.length()}")

                if (bodies.length() > 0) {

                    val body = bodies.getJSONObject(0)

                    deadBodyId = body.getString("victimId")
                    deadBodyLat = body.getDouble("lat")
                    deadBodyLng = body.getDouble("lng")

                    Log.d(
                        "BODY_RESTORED",
                        "Body of $deadBodyId at $deadBodyLat,$deadBodyLng"
                    )
                }
            }
        })
    }


    override fun onDestroy() {

        super.onDestroy()

        val socket = SocketManager.getSocket()

        socket?.off("game:player-moved")
        socket?.off("game:nearby-targets")
        socket?.off("game:kill-event")

        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}