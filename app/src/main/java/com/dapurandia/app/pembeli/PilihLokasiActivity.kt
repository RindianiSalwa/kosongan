package com.dapurandia.app.pembeli

import android.app.AlertDialog
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dapurandia.app.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale

class PilihLokasiActivity : AppCompatActivity() {

    private lateinit var layoutLoading: FrameLayout
    private lateinit var mapView: MapView
    private lateinit var btnBack: ImageButton
    private lateinit var tvAlamat: TextView
    private lateinit var btnPakaiLokasi: Button
    private lateinit var imgPin: ImageView
    private lateinit var layoutSearchTrigger: MaterialCardView
    private lateinit var editNoRumah: TextInputEditText
    private lateinit var editPatokan: TextInputEditText

    private val handler = Handler(Looper.getMainLooper())
    private val updateAddressRunnable = Runnable { updateSelectedLocationFromMap() }

    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var latAwal: Double? = null
    private var lngAwal: Double? = null
    private var alamatAwal: String? = null

    private val purwakartaBounds = BoundingBox(
        -6.4850,
        107.5020,
        -6.6133,
        107.3945
    )
    private val tamanAirMancur = GeoPoint(-6.5626, 107.4457)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.pembeli_activity_pilih_lokasi)

        layoutLoading = findViewById(R.id.layoutLoading)
        mapView = findViewById(R.id.map)
        btnBack = findViewById(R.id.btnBack)
        tvAlamat = findViewById(R.id.editAlamat)
        btnPakaiLokasi = findViewById(R.id.btnPakaiLokasi)
        imgPin = findViewById(R.id.imgPin)
        layoutSearchTrigger = findViewById(R.id.layoutSearchTrigger)
        editNoRumah = findViewById(R.id.editNoRumahPilih)
        editPatokan = findViewById(R.id.editPatokanPilih)

        latAwal = intent.getDoubleExtra("LAT_AWAL", Double.NaN).takeIf { !it.isNaN() }
        lngAwal = intent.getDoubleExtra("LNG_AWAL", Double.NaN).takeIf { !it.isNaN() }
        alamatAwal = intent.getStringExtra("ALAMAT")

        setupMap()
        setupActions()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateAddressRunnable)
        super.onDestroy()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 13.0
        mapView.maxZoomLevel = 20.0
        mapView.setScrollableAreaLimitDouble(purwakartaBounds)

        val startPoint = if (latAwal != null && lngAwal != null) {
            GeoPoint(latAwal!!, lngAwal!!)
        } else {
            tamanAirMancur
        }

        selectedLat = startPoint.latitude
        selectedLng = startPoint.longitude
        tvAlamat.text = alamatAwal.takeUnless { it.isNullOrBlank() } ?: "Geser peta untuk memilih lokasi"

        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(startPoint)
        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                animatePinUp()
                scheduleAddressUpdate()
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                scheduleAddressUpdate()
                return false
            }
        })

        layoutLoading.animate()
            .alpha(0f)
            .setDuration(350)
            .withEndAction { layoutLoading.visibility = View.GONE }
            .start()
    }

    private fun setupActions() {
        btnBack.setOnClickListener { finish() }
        layoutSearchTrigger.setOnClickListener { showSearchDialog() }

        btnPakaiLokasi.setOnClickListener {
            val lat = selectedLat
            val lng = selectedLng
            if (lat == null || lng == null) {
                Toast.makeText(this, "Lokasi belum dipilih", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val noRumah = editNoRumah.text?.toString()?.trim().orEmpty()
            val patokan = editPatokan.text?.toString()?.trim().orEmpty()
            val detailString = when {
                noRumah.isEmpty() && patokan.isEmpty() -> "-"
                noRumah.isNotEmpty() && patokan.isNotEmpty() -> "No. $noRumah, $patokan"
                noRumah.isNotEmpty() -> "No. $noRumah"
                else -> patokan
            }

            val result = Intent().apply {
                putExtra("LAT", lat)
                putExtra("LNG", lng)
                putExtra("ALAMAT", tvAlamat.text.toString())
                putExtra("ALAMAT_DETAIL", detailString)
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }

    private fun scheduleAddressUpdate() {
        handler.removeCallbacks(updateAddressRunnable)
        handler.postDelayed(updateAddressRunnable, 500)
    }

    private fun updateSelectedLocationFromMap() {
        animatePinDown()
        val center = mapView.mapCenter as? GeoPoint ?: return
        if (!isInsidePurwakarta(center)) {
            Toast.makeText(this, "Lokasi di luar jangkauan Purwakarta!", Toast.LENGTH_SHORT).show()
            tvAlamat.text = "Lokasi di luar jangkauan"
            btnPakaiLokasi.isEnabled = false
            btnPakaiLokasi.alpha = 0.5f
            return
        }

        selectedLat = center.latitude
        selectedLng = center.longitude
        btnPakaiLokasi.isEnabled = true
        btnPakaiLokasi.alpha = 1f
        tvAlamat.text = getAddress(center.latitude, center.longitude)
    }

    private fun showSearchDialog() {
        val input = EditText(this).apply {
            hint = "Cari lokasi di Purwakarta"
            setSingleLine(true)
        }

        AlertDialog.Builder(this)
            .setTitle("Cari Lokasi")
            .setView(input)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Cari") { _, _ ->
                searchLocation(input.text.toString().trim())
            }
            .show()
    }

    private fun searchLocation(query: String) {
        if (query.isBlank()) return

        try {
            val results = Geocoder(this, Locale.getDefault())
                .getFromLocationName("$query, Purwakarta, Jawa Barat", 5)
                .orEmpty()
            val match = results.firstOrNull {
                isInsidePurwakarta(GeoPoint(it.latitude, it.longitude))
            }

            if (match == null) {
                Toast.makeText(this, "Lokasi tidak ditemukan di area Purwakarta.", Toast.LENGTH_SHORT).show()
                return
            }

            val point = GeoPoint(match.latitude, match.longitude)
            selectedLat = point.latitude
            selectedLng = point.longitude
            tvAlamat.text = match.getAddressLine(0) ?: query
            btnPakaiLokasi.isEnabled = true
            btnPakaiLokasi.alpha = 1f
            mapView.controller.animateTo(point, 17.0, 600L)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal mencari lokasi.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAddress(lat: Double, lng: Double): String {
        return try {
            val addresses = Geocoder(this, Locale.getDefault()).getFromLocation(lat, lng, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: "Alamat tidak ditemukan"
        } catch (e: Exception) {
            "Gagal memuat alamat"
        }
    }

    private fun isInsidePurwakarta(point: GeoPoint): Boolean {
        return purwakartaBounds.contains(point)
    }

    private fun animatePinUp() {
        imgPin.animate().translationY(-70f).setDuration(180).start()
    }

    private fun animatePinDown() {
        imgPin.animate().translationY(-20f).setDuration(220).start()
    }
}
