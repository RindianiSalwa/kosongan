package com.dapurandia.app.pembeli

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dapurandia.app.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale

class LokasiSaatIniActivity : AppCompatActivity() {

    private lateinit var layoutLoading: FrameLayout
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var textAlamatMap: TextView
    private lateinit var imgPin: ImageView
    private lateinit var imgPinShadow: ImageView
    private val handler = Handler(Looper.getMainLooper())

    private var isFirstLocationFound = false
    private var isUserMoveMap = false
    private var lat = 0.0
    private var lng = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.pembeli_activity_lokasi_saat_ini)

        layoutLoading = findViewById(R.id.layoutLoading)
        mapView = findViewById(R.id.map)
        textAlamatMap = findViewById(R.id.textAlamatMap)
        imgPin = findViewById(R.id.imgPin)
        imgPinShadow = findViewById(R.id.imgPinShadow)

        val editNoRumah = findViewById<TextInputEditText>(R.id.editNoRumah)
        val editPatokan = findViewById<TextInputEditText>(R.id.editPatokan)
        val btnSimpan = findViewById<MaterialButton>(R.id.btnSimpanLokasi)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        textAlamatMap.text = "Geser peta untuk menentukan alamat"

        setupMap()
        startLocationUpdatesIfAllowed()

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
                putExtra("ALAMAT", textAlamatMap.text.toString())
                putExtra("ALAMAT_DETAIL", alamatDetail)
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        handler.removeCallbacksAndMessages(null)
        super.onStop()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 13.0
        mapView.maxZoomLevel = 20.0
        mapView.controller.setZoom(16.0)
        val defaultPoint = GeoPoint(CheckoutActivity.LAT_DAPUR, CheckoutActivity.LNG_DAPUR)
        lat = defaultPoint.latitude
        lng = defaultPoint.longitude
        mapView.controller.setCenter(defaultPoint)

        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                isUserMoveMap = true
                animatePinUp()
                updateFromMapCenter()
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                updateFromMapCenter()
                return false
            }
        })
    }

    private fun startLocationUpdatesIfAllowed() {
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
            textAlamatMap.text = "Izin lokasi diperlukan. Kamu tetap bisa geser peta untuk memilih alamat."
            tutupLoading()
            return
        }

        handler.postDelayed({
            if (!isFirstLocationFound) {
                textAlamatMap.text = "Lokasi belum terdeteksi. Geser peta ke alamat pengantaran."
                updateFromMapCenter()
                tutupLoading()
                isFirstLocationFound = true
            }
        }, 6000)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (!isFirstLocationFound && location != null && isValidLocation(location)) {
                    applyLocation(location)
                }
            }
            .addOnFailureListener {
                if (!isFirstLocationFound) {
                    textAlamatMap.text = "Lokasi belum terdeteksi. Geser peta ke alamat pengantaran."
                    updateFromMapCenter()
                    tutupLoading()
                    isFirstLocationFound = true
                }
            }

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).setMinUpdateIntervalMillis(1000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (!isValidLocation(location)) return
                if (isUserMoveMap) return

                applyLocation(location)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun isValidLocation(location: Location): Boolean {
        return location.latitude != 0.0 || location.longitude != 0.0
    }

    private fun applyLocation(location: Location) {
        lat = location.latitude
        lng = location.longitude
        val userPoint = GeoPoint(lat, lng)
        mapView.controller.setZoom(17.0)
        mapView.controller.animateTo(userPoint)
        tampilkanAlamatDariMap(lat, lng)
        tutupLoading()
        isFirstLocationFound = true
    }

    private fun updateFromMapCenter() {
        val center = mapView.mapCenter as? GeoPoint ?: return
        lat = center.latitude
        lng = center.longitude
        tampilkanAlamatDariMap(lat, lng)
        animatePinDown()
    }

    private fun tampilkanAlamatDariMap(lat: Double, lng: Double) {
        try {
            val alamatList = Geocoder(this, Locale.getDefault()).getFromLocation(lat, lng, 1)
            textAlamatMap.text = alamatList?.firstOrNull()?.getAddressLine(0) ?: "Alamat tidak ditemukan"
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

        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdatesIfAllowed()
        }
    }

    private fun tutupLoading() {
        layoutLoading.animate()
            .alpha(0f)
            .setDuration(350)
            .withEndAction { layoutLoading.visibility = View.GONE }
            .start()
    }
}
