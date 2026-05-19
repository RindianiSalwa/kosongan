package com.dapurandia.app.pembeli

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dapurandia.app.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.view.View
import android.widget.FrameLayout

class LokasiSaatIniActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var layoutLoading: FrameLayout
    private var isFirstLocationFound = false
    private lateinit var mMap: GoogleMap
    private var lat = 0.0
    private var lng = 0.0

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var textAlamatMap: TextView

    // 🔥 PIN
    private lateinit var imgPin: ImageView
    private lateinit var imgPinShadow: ImageView
    private var isUserMoveMap = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pembeli_activity_lokasi_saat_ini)

        layoutLoading = findViewById(R.id.layoutLoading)
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        textAlamatMap = findViewById(R.id.textAlamatMap)
        val editNoRumah = findViewById<TextInputEditText>(R.id.editNoRumah)
        val editPatokan = findViewById<TextInputEditText>(R.id.editPatokan)
        val btnSimpan = findViewById<MaterialButton>(R.id.btnSimpanLokasi)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        imgPin = findViewById(R.id.imgPin)
        imgPinShadow = findViewById(R.id.imgPinShadow)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        textAlamatMap.text = "Geser peta untuk menentukan alamat"

        btnBack.setOnClickListener { finish() }

        btnSimpan.setOnClickListener {
            if (lat == 0.0 || lng == 0.0) {
                Toast.makeText(this, "Lokasi belum siap", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val noRumah = editNoRumah.text?.toString()?.trim().orEmpty()
            val patokan = editPatokan.text?.toString()?.trim().orEmpty()

            val alamatDetail = when {
                noRumah.isEmpty() && patokan.isEmpty() -> "-"
                noRumah.isNotEmpty() && patokan.isNotEmpty() -> "No. $noRumah, $patokan"
                noRumah.isNotEmpty() -> "No. $noRumah"
                else -> patokan
            }

            val result = Intent().apply {
                putExtra("LAT", lat)
                putExtra("LNG", lng)
                putExtra("ALAMAT", textAlamatMap.text.toString()) // 🔥 TAMBAHKAN INI
                putExtra("ALAMAT_DETAIL", alamatDetail)
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        mMap.isMyLocationEnabled = true

        // ✅ REALTIME LOCATION (ANTI CACHE)
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000L
        ).setMinUpdateIntervalMillis(1000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (location.latitude == 0.0 && location.longitude == 0.0) return

                if (isUserMoveMap) return
                lat = location.latitude
                lng = location.longitude

                val userLatLng = LatLng(lat, lng)
                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(userLatLng, 17f)
                )

                tampilkanAlamatDariMap(lat, lng)
                if (!isFirstLocationFound) {
                    tutupLoading()
                    isFirstLocationFound = true
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // 🔥 PIN ANIMATION
        mMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isUserMoveMap = true
                animatePinUp()
            }
        }

        mMap.setOnCameraIdleListener {
            animatePinDown()
            if (!isUserMoveMap) return@setOnCameraIdleListener

            val center = mMap.cameraPosition.target
            lat = center.latitude
            lng = center.longitude

            tampilkanAlamatDariMap(lat, lng)
        }
    }

    override fun onStop() {
        super.onStop()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun tampilkanAlamatDariMap(lat: Double, lng: Double) {
        try {
            val geocoder = Geocoder(this, java.util.Locale.getDefault())
            val alamatList = geocoder.getFromLocation(lat, lng, 1)

            if (!alamatList.isNullOrEmpty()) {
                textAlamatMap.text = alamatList[0].getAddressLine(0)
            } else {
                textAlamatMap.text = "Alamat tidak ditemukan"
            }
        } catch (e: Exception) {
            textAlamatMap.text = "Gagal memuat alamat"
        }
    }

    private fun animatePinUp() {
        imgPin.animate().translationY(-70f).setDuration(180).start()
        imgPinShadow.animate().scaleX(0.6f).scaleY(0.6f).alpha(0.2f).setDuration(180).start()
    }

    private fun animatePinDown() {
        imgPin.animate().translationY(-20f).setDuration(220).start()
        imgPinShadow.animate().scaleX(1f).scaleY(1f).alpha(0.4f).setDuration(220).start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            onMapReady(mMap)
        }
    }
    private fun tutupLoading() {
        layoutLoading.animate()
            .alpha(0f)
            .setDuration(500)
            .withEndAction {
                layoutLoading.visibility = View.GONE
            }
    }
}
