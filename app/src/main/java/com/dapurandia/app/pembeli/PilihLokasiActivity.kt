package com.dapurandia.app.pembeli

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dapurandia.app.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import android.view.View
import android.widget.FrameLayout

class PilihLokasiActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var layoutLoading: FrameLayout
    private lateinit var mMap: GoogleMap
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var isUserMoveMap = false

    // View sesuai layout baru
    private lateinit var btnBack: ImageButton
    private lateinit var tvAlamat: TextView
    private lateinit var btnPakaiLokasi: Button
    private lateinit var imgPin: ImageView
    private lateinit var layoutSearchTrigger: MaterialCardView
    private lateinit var editNoRumah: TextInputEditText
    private lateinit var editPatokan: TextInputEditText

    private val AUTOCOMPLETE_REQUEST_CODE = 1001

    // Data awal
    private var latAwal: Double? = null
    private var lngAwal: Double? = null
    private var alamatAwal: String? = null

    private val PURWAKARTA_BOUNDS = LatLngBounds(
        LatLng(-6.6133, 107.3945), // South-West (Pojok Kiri Bawah)
        LatLng(-6.4850, 107.5020)  // North-East (Pojok Kanan Atas)
    )
    private val TAMAN_AIR_MANCUR = LatLng(-6.5626, 107.4457)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pembeli_activity_pilih_lokasi)

        layoutLoading = findViewById(R.id.layoutLoading)
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Menutup activity dan kembali ke halaman sebelumnya
        }
        // 1. Inisialisasi View
        tvAlamat = findViewById(R.id.editAlamat)
        btnPakaiLokasi = findViewById(R.id.btnPakaiLokasi)
        imgPin = findViewById(R.id.imgPin)
        layoutSearchTrigger = findViewById(R.id.layoutSearchTrigger)
        editNoRumah = findViewById(R.id.editNoRumahPilih)
        editPatokan = findViewById(R.id.editPatokanPilih)

        // 2. Ambil data dari Intent
        latAwal = intent.getDoubleExtra("LAT_AWAL", Double.NaN).takeIf { !it.isNaN() }
        lngAwal = intent.getDoubleExtra("LNG_AWAL", Double.NaN).takeIf { !it.isNaN() }
        alamatAwal = intent.getStringExtra("ALAMAT")

        // 3. Inisialisasi Places
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        // 4. Set alamat awal jika ada
        if (!alamatAwal.isNullOrEmpty()) {
            tvAlamat.text = alamatAwal
        }

        // 5. Klik Search Bar (CardView melayang)
        layoutSearchTrigger.setOnClickListener {
            bukaAutocompletePlaces()
        }

        // 6. Klik Tombol Pakai Lokasi
        // Di dalam onCreate PilihLokasiActivity.kt
        btnPakaiLokasi.setOnClickListener {
            if (selectedLat != null && selectedLng != null) {
                // Ambil input detail dari layout baru yang kita buat tadi
                val editNoRumah = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editNoRumahPilih)
                val editPatokan = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editPatokanPilih)

                val noRumah = editNoRumah.text?.toString()?.trim().orEmpty()
                val patokan = editPatokan.text?.toString()?.trim().orEmpty()

                // Susun string detailnya
                val detailString = when {
                    noRumah.isEmpty() && patokan.isEmpty() -> "-"
                    noRumah.isNotEmpty() && patokan.isNotEmpty() -> "No. $noRumah, $patokan"
                    noRumah.isNotEmpty() -> "No. $noRumah"
                    else -> patokan
                }

                val result = Intent().apply {
                    putExtra("LAT", selectedLat)
                    putExtra("LNG", selectedLng)
                    putExtra("ALAMAT", tvAlamat.text.toString())
                    // 🔥 TAMBAHKAN INI: Supaya ditangkap oleh BottomSheet
                    putExtra("ALAMAT_DETAIL", detailString)
                }
                setResult(RESULT_OK, result)
                finish()
            }
        }
        // 7. Load Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        layoutLoading.animate()
            .alpha(0f)
            .setDuration(500)
            .withEndAction {
                layoutLoading.visibility = View.GONE
            }
        mMap.setLatLngBoundsForCameraTarget(PURWAKARTA_BOUNDS)

        val posisiAwal = if (latAwal != null && lngAwal != null) {
            LatLng(latAwal!!, lngAwal!!)
        } else {
            TAMAN_AIR_MANCUR
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posisiAwal, 16f))

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

            // CEK: Apakah lokasi pin ada di dalam area Purwakarta?
            if (PURWAKARTA_BOUNDS.contains(center)) {
                selectedLat = center.latitude
                selectedLng = center.longitude

                // Update alamat jika di dalam area
                try {
                    val geocoder = Geocoder(this)
                    val hasil = geocoder.getFromLocation(center.latitude, center.longitude, 1)
                    if (!hasil.isNullOrEmpty()) {
                        tvAlamat.text = hasil[0].getAddressLine(0)
                        btnPakaiLokasi.isEnabled = true
                        btnPakaiLokasi.alpha = 1f
                    }
                } catch (e: Exception) { e.printStackTrace() }
            } else {
                // Jika di luar area, beri peringatan dan matikan tombol
                Toast.makeText(this, "Lokasi di luar jangkauan Purwakarta!", Toast.LENGTH_SHORT).show()
                tvAlamat.text = "Lokasi di luar jangkauan"
                btnPakaiLokasi.isEnabled = false
                btnPakaiLokasi.alpha = 0.5f
            }
        }
    }

    private fun bukaAutocompletePlaces() {
        layoutLoading.visibility = View.VISIBLE
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            // Ganti Bias menjadi Restriction agar hasil WAJIB di dalam bounds
            .setLocationRestriction(RectangularBounds.newInstance(PURWAKARTA_BOUNDS))
            .setCountries(listOf("ID"))
            .build(this)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }
    override fun onDestroy() {
        // Pastikan tidak ada memory leak dari Places
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val place = Autocomplete.getPlaceFromIntent(data)
            val latLng = place.latLng ?: return

            // VALIDASI MANUAL: Cek apakah hasil search benar-benar di dalam bounds
            if (PURWAKARTA_BOUNDS.contains(latLng)) {
                isUserMoveMap = false
                tvAlamat.text = place.address
                selectedLat = latLng.latitude
                selectedLng = latLng.longitude
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))

                btnPakaiLokasi.isEnabled = true
                btnPakaiLokasi.alpha = 1f
            } else {
                // Jika Google "kecolongan" kasih hasil luar kota, kita blokir di sini
                Toast.makeText(this, "Maaf, lokasi ini di luar jangkauan layanan kami.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun animatePinUp() {
        imgPin.animate().translationY(-70f).setDuration(180).start()
    }

    private fun animatePinDown() {
        imgPin.animate().translationY(-20f).setDuration(220).start()
    }

}