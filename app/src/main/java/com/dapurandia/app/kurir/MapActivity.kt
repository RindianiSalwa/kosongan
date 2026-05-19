package com.dapurandia.app.kurir

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.dapurandia.app.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.material.materialswitch.MaterialSwitch

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var isRealGPS = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var kurirLatLng: LatLng
    private lateinit var tujuanLatLng: LatLng
    private var routePolyline: Polyline? = null
    private lateinit var googleMap: GoogleMap
    private var routePoints: List<LatLng> = emptyList()
    private var kurirMarker: Marker? = null

    private lateinit var imagePembeli: ImageView
    private lateinit var textNamaPembeli: TextView
    private lateinit var textNoHpPembeli: TextView
    private lateinit var btnMulai: Button
    private val firestore = FirebaseFirestore.getInstance()

    private var destLat: Double = 0.0
    private var destLng: Double = 0.0
    private var originLat: Double? = null
    private var originLng: Double? = null
    private var navigationStarted = false
    private var currentRouteIndex = 0
    private var isNavigating = false
    private var grayPolyline: Polyline? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kurir_activity_map)

        // Inisialisasi Fused Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        imagePembeli = findViewById(R.id.imagePembeli)
        textNamaPembeli = findViewById(R.id.textNamaPembeli)
        textNoHpPembeli = findViewById(R.id.textNoHpPembeli)
        btnMulai = findViewById(R.id.btnMulai)
        btnMulai.isEnabled = false

        val switchGPS = findViewById<MaterialSwitch>(R.id.switchModeGPS)
        switchGPS.setOnCheckedChangeListener { _, isChecked ->
            isRealGPS = isChecked
            if (isRealGPS) {
                stopNavigation()
                // Langsung cari lokasi sekarang dan gambar ulang rute
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                        loc?.let { setupInitialMarkerAndRoute(LatLng(it.latitude, it.longitude)) }
                    }
                }
                startRealTracking()
            } else {
                stopRealTracking()
                // Balikkan ke rute awal (Dapur Andia) jika mode dummy
                val startLat = originLat ?: destLat
                val startLng = originLng ?: destLng
                setupInitialMarkerAndRoute(LatLng(startLat, startLng))
            }
        }

        val btnCancel: ImageView = findViewById(R.id.btnCancel)
        btnCancel.setOnClickListener {
            val pesananId = intent.getStringExtra("idPesanan")
            if (pesananId != null) {
                firestore.collection("pesanan").document(pesananId)
                    .update("status", "siap_diantar")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Navigasi dibatalkan", Toast.LENGTH_SHORT).show()
                        stopNavigation()
                        finish()
                    }
                    .addOnFailureListener { finish() }
            } else {
                finish()
            }
        }

        val idPembeli = intent.getStringExtra("idPembeli") ?: return
        loadProfilePembeli(idPembeli)

        destLat = intent.getDoubleExtra("lat", 0.0)
        destLng = intent.getDoubleExtra("lng", 0.0)
        originLat = if (intent.hasExtra("originLat")) intent.getDoubleExtra("originLat", 0.0) else null
        originLng = if (intent.hasExtra("originLng")) intent.getDoubleExtra("originLng", 0.0) else null

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnMulai.setOnClickListener {
            val statusTombol = btnMulai.text.toString().lowercase()
            val pesananId = intent.getStringExtra("idPesanan") ?: return@setOnClickListener

            if (statusTombol == "mulai") {
                if (!navigationStarted) {
                    firestore.collection("pesanan").document(pesananId)
                        .update("status", "diantar")
                        .addOnSuccessListener {
                            navigationStarted = true
                            if (isRealGPS) {
                                startRealTracking()
                            } else {
                                startNavigation()
                            }
                            btnMulai.visibility = View.GONE
                        }
                }
            } else if (statusTombol == "pesanan selesai") {
                val dialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Berhasil")
                    .setContentText("Pesanan telah diselesaikan")
                dialog.hideConfirmButton()
                dialog.show()

                firestore.collection("pesanan").document(pesananId)
                    .update(mapOf(
                        "status" to "selesai",
                        "waktuSelesai" to com.google.firebase.Timestamp.now()
                    ))
                    .addOnSuccessListener {
                        Handler(Looper.getMainLooper()).postDelayed({
                            dialog.dismissWithAnimation()
                            finish()
                        }, 1500)
                    }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isMyLocationButtonEnabled = false

        tujuanLatLng = LatLng(destLat, destLng)
        googleMap.addMarker(MarkerOptions().position(tujuanLatLng).title("Tujuan"))

        // Jika mode Real GPS aktif, kita coba ambil lokasi asli HP dulu
        if (isRealGPS) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val currentPos = LatLng(location.latitude, location.longitude)
                        setupInitialMarkerAndRoute(currentPos)
                    } else {
                        // Fallback jika GPS HP belum dapet lock lokasi
                        val startLat = originLat ?: destLat
                        val startLng = originLng ?: destLng
                        setupInitialMarkerAndRoute(LatLng(startLat, startLng))
                    }
                }
            }
        } else {
            // Mode Dummy: Gunakan koordinat Dapur Andia (originLat)
            val startLat = originLat ?: destLat
            val startLng = originLng ?: destLng
            setupInitialMarkerAndRoute(LatLng(startLat, startLng))
        }
    }

    // Fungsi bantuan agar kode tidak duplikat
    private fun setupInitialMarkerAndRoute(startPos: LatLng) {
        kurirLatLng = startPos

        // Hapus marker lama jika ada
        kurirMarker?.remove()

        kurirMarker = googleMap.addMarker(
            MarkerOptions()
                .position(kurirLatLng)
                .title("Posisi Kurir")
                .icon(bitmapDescriptorFromVector(R.drawable.ic_user_location))
                .anchor(0.5f, 0.5f)
                .flat(true)
        )

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kurirLatLng, 16f))
        getDirections(kurirLatLng.latitude, kurirLatLng.longitude, tujuanLatLng.latitude, tujuanLatLng.longitude)
    }

    private fun startNavigation() {
        if (routePoints.isEmpty()) return

        isNavigating = true
        kurirMarker?.setIcon(bitmapDescriptorFromVector(R.drawable.ic_navigation_arrow))

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder().target(kurirLatLng).zoom(19f).tilt(40f).build()
        ))

        currentRouteIndex = 0
        moveKurirAlongRoute()
    }

    private fun moveKurirAlongRoute() {
        if (!isNavigating || routePoints.isEmpty() || isRealGPS) return

        if (currentRouteIndex >= routePoints.size) {
            isNavigating = false
            runOnUiThread {
                btnMulai.visibility = View.VISIBLE
                btnMulai.text = "Pesanan Selesai"
                btnMulai.setBackgroundColor(getColor(R.color.green))
            }
            return
        }

        val nextPos = routePoints[currentRouteIndex]
        val prevPos = if (currentRouteIndex > 0) routePoints[currentRouteIndex - 1] else kurirMarker?.position ?: nextPos
        val bearing = getBearing(prevPos, nextPos)

        val pesananId = intent.getStringExtra("idPesanan")
        if (pesananId != null) {
            firestore.collection("pesanan").document(pesananId)
                .update(mapOf(
                    "latKurir" to nextPos.latitude, // Disamakan dengan field Pembeli
                    "lngKurir" to nextPos.longitude,
                    "bearing" to bearing
                ))
        }

        kurirMarker?.position = nextPos
        kurirMarker?.rotation = bearing
        updateRoutePolylines(currentRouteIndex)

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder().target(nextPos).zoom(19f).bearing(bearing).tilt(40f).build()
        ))

        currentRouteIndex++
        handler.postDelayed({ moveKurirAlongRoute() }, 3000)
    }

    private fun startRealTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (!isRealGPS) return
                val location = result.lastLocation ?: return
                val currentPos = LatLng(location.latitude, location.longitude)
                val bearing = location.bearing

                kurirMarker?.position = currentPos
                kurirMarker?.rotation = bearing
                kurirMarker?.setIcon(bitmapDescriptorFromVector(R.drawable.ic_navigation_arrow))
                val cameraPosition = CameraPosition.Builder()
                    .target(currentPos)
                    .zoom(18f)
                    .bearing(location.bearing)
                    .tilt(40f)
                    .build()
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                val pesananId = intent.getStringExtra("idPesanan") ?: return
                firestore.collection("pesanan").document(pesananId)
                    .update(mapOf(
                        "latKurir" to location.latitude,
                        "lngKurir" to location.longitude,
                        "bearing" to bearing
                    ))
                updateRoutePolylinesRealtime(currentPos)

                // Update rute & ETA berdasarkan posisi GPS asli
                updateRealtimeETAFromGPS(currentPos)
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }
    private fun updateRoutePolylines(index: Int) {
        if (routePoints.isEmpty() || index >= routePoints.size) return

        val pointsDone = routePoints.subList(0, index + 1)
        val pointsRemaining = routePoints.subList(index, routePoints.size)

        if (grayPolyline == null) {
            grayPolyline = googleMap.addPolyline(PolylineOptions()
                .addAll(pointsDone)
                .width(14f) // Samakan ketebalannya
                .color(Color.LTGRAY)
                .jointType(JointType.ROUND))
        } else {
            grayPolyline?.points = pointsDone
        }
        routePolyline?.points = pointsRemaining
    }

    private fun updateRealtimeETAFromGPS(currentPos: LatLng) {
        // Fungsi ini mirip updateRealtimeETA tapi inputnya koordinat GPS
        val apiKey = getString(R.string.google_directions_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${currentPos.latitude},${currentPos.longitude}" +
                "&destination=${tujuanLatLng.latitude},${tujuanLatLng.longitude}" +
                "&mode=driving&key=$apiKey"

        OkHttpClient().newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { json ->
                    val obj = JSONObject(json)
                    if (obj.getString("status") == "OK") {
                        val leg = obj.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0)
                        val jarak = leg.getJSONObject("distance").getString("text")
                        val waktu = leg.getJSONObject("duration").getString("text")

                        runOnUiThread {
                            findViewById<TextView>(R.id.tvJarak).text = jarak
                            findViewById<TextView>(R.id.tvWaktu).text = waktu
                        }

                        val pesananId = intent.getStringExtra("idPesanan")
                        if (pesananId != null) {
                            firestore.collection("pesanan").document(pesananId)
                                .update(mapOf("estimasiJarak" to jarak, "estimasiWaktu" to waktu))
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {}
        })
    }

    private fun stopRealTracking() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun stopNavigation() {
        isNavigating = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun getDirections(originLat: Double, originLng: Double, destLat: Double, destLng: Double) {
        val apiKey = getString(R.string.google_directions_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$originLat,$originLng&destination=$destLat,$destLng&mode=driving&key=$apiKey"

        OkHttpClient().newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { json ->
                    val obj = JSONObject(json)
                    if (obj.getString("status") == "OK") {
                        val routes = obj.getJSONArray("routes")
                        val overviewPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points")
                        routePoints = PolyUtil.decode(overviewPolyline)
                        runOnUiThread {
                            routePolyline?.remove()
                            routePolyline = googleMap.addPolyline(PolylineOptions().addAll(routePoints).width(12f).color(Color.parseColor("#1A73E8")))
                            btnMulai.isEnabled = true
                        }
                    }
                }
            }
        })
    }

    // Fungsi pembantu lainnya tetap sama
    private fun loadProfilePembeli(idPembeli: String) {
        firestore.collection("pembeli").document(idPembeli).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                textNamaPembeli.text = doc.getString("nama") ?: "-"
                textNoHpPembeli.text = doc.getString("no_hp") ?: "-"
                Glide.with(this).load(doc.getString("fotoProfil")).placeholder(R.drawable.ic_default_profile).into(imagePembeli)
            }
        }
    }

    private fun bitmapDescriptorFromVector(vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(this, vectorResId)
        val bitmap = android.graphics.Bitmap.createBitmap(vectorDrawable!!.intrinsicWidth, vectorDrawable.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun updateRoutePolylinesRealtime(currentPos: LatLng) {
        if (routePoints.isEmpty()) return

        // Cari index titik rute yang paling dekat dengan lokasi GPS HP saat ini
        val closestIndex = routePoints.withIndex().minByOrNull {
            val result = FloatArray(1)
            android.location.Location.distanceBetween(
                it.value.latitude, it.value.longitude,
                currentPos.latitude, currentPos.longitude,
                result
            )
            result[0]
        }?.index ?: return

        val pointsDone = routePoints.subList(0, closestIndex + 1)
        val pointsRemaining = routePoints.subList(closestIndex, routePoints.size)

        // Update Garis Abu-abu (Sudah dilewati)
        if (grayPolyline == null) {
            grayPolyline = googleMap.addPolyline(PolylineOptions()
                .addAll(pointsDone)
                .width(14f)
                .color(Color.LTGRAY)
                .startCap(RoundCap())
                .endCap(RoundCap())
                .jointType(JointType.ROUND))
        } else {
            grayPolyline?.points = pointsDone
        }

        // Update Garis Biru (Sisa rute)
        routePolyline?.points = pointsRemaining
    }

    fun getBearing(from: LatLng, to: LatLng): Float {
        val lat1 = Math.toRadians(from.latitude); val lon1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(to.latitude); val lon2 = Math.toRadians(to.longitude)
        val dLon = lon2 - lon1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
        return Math.toDegrees(Math.atan2(y, x)).toFloat()
    }

    override fun onDestroy() {
        stopNavigation()
        stopRealTracking()
        super.onDestroy()
    }
}