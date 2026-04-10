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
import com.example.uploadingscreen.network.GameEndHandler
import com.google.android.gms.location.*
import org.json.JSONObject
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

data class DeadBody(
    val victimId: String,
    val lat: Double,
    val lng: Double
)

class GameActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityGameBinding

    private val playerMap = mutableMapOf<String, String>()

    private var roomCode: String? = null
    private var role: String? = null

    private var currentVictimId: String? = null

    private val deadBodies = mutableListOf<DeadBody>()
    private var reportTargetBody: DeadBody? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var taskHandler : TaskHandler
    private lateinit var lifeHandler : GameEndHandler

    private lateinit var mMap: GoogleMap
    private val playerMarkers = mutableMapOf<String, Marker>()
    private var myMarker: Marker? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 101
        private const val REPORT_RANGE_METRES = 8f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

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


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
       mMap.uiSettings.isZoomControlsEnabled = true
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        }
    }


    private fun setupLocation() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation ?: return

                val lat = location.latitude
                val lng = location.longitude

                sendMove(lat, lng)

                binding.tvMyPosition.text = "My Position: $lat , $lng"

             //used for markers
                if (::mMap.isInitialized) {
                    val latLng = LatLng(lat, lng)

                    if (myMarker == null) {
                        myMarker = mMap.addMarker(
                            MarkerOptions().position(latLng).title("You")
                        )
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                    } else {
                        myMarker?.position = latLng
                    }
                }

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

    // move socket
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

    // player move listener
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

                    //other player markers
                    if (::mMap.isInitialized) {
                        val latLng = LatLng(lat, lng)

                        if (playerMarkers.containsKey(userId)) {
                            playerMarkers[userId]?.position = latLng
                        } else {
                            val marker = mMap.addMarker(
                                MarkerOptions().position(latLng).title(username)
                            )
                            marker?.let { playerMarkers[userId] = it }
                        }
                    }

                    Log.d("PLAYER_MOVED", "$username moved to $lat,$lng")
                }
            }
        }
    }

    //target socket
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

                    } else {
                        currentVictimId = null
                    }
                }
            }
        }
    }

    // KILL
    private fun sendKill(victimId: String) {

        val socket = SocketManager.getSocket() ?: return

        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("victimId", victimId)
        }

        socket.emit("game:kill", payload)

        binding.btnKill.isEnabled = false
    }

    // Kill event
    private fun listenForKillEvent() {

        val socket = SocketManager.getSocket() ?: return

        socket.off("game:kill-event")

        socket.on("game:kill-event") { args ->

            if (args.isNotEmpty() && args[0] is JSONObject) {

                val data = args[0] as JSONObject

                val victimId = data.getString("victimId")

                val position = data.getJSONObject("position")

                val lat = position.getDouble("lat")
                val lng = position.getDouble("lng")

                deadBodies.add(DeadBody(victimId, lat, lng))

                // dead body marker
                if (::mMap.isInitialized) {
                    mMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title("Dead Body")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }
            }
        }
    }

    // REPORT
    private fun sendReportBody() {
        val victimId = reportTargetBody?.victimId ?: return
        val socket = SocketManager.getSocket() ?: return

        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("bodyVictimId", victimId)
        }

        socket.emit("game:report-body", payload)
    }

    // Body range
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
        } else {
            reportTargetBody = null
            binding.btnReport.visibility = View.GONE
        }
    }

    // GET BODIES
    private fun requestBodies() {
        val socket = SocketManager.getSocket() ?: return

        val payload = JSONObject().apply {
            put("roomCode", roomCode)
        }

        socket.emit("game:get-bodies", payload)
    }

    // MEETING
    private fun listenMeetingStart() {

        val socket = SocketManager.getSocket() ?: return

        socket.off("game:meeting-started")

        socket.on("game:meeting-started") { args ->

            val data = args[0] as JSONObject

            runOnUiThread {

                fusedLocationClient.removeLocationUpdates(locationCallback)

                val intent = Intent(this, MeetingActivity::class.java)
                intent.putExtra("roomCode", roomCode)

                startActivity(intent)
            }
        }
    }

    override fun onResume(){
        super.onResume()
        if(::fusedLocationClient.isInitialized){
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

    // ✅ PERMISSION FIX
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }
}