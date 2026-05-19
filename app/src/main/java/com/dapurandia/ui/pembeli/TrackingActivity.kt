package com.example.dapurandia.ui.pembeli

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dapurandia.R

class PengirimanActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengiriman)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val pesananId = intent.getStringExtra("pesananId")!!

        btnAmbilMakanan.setOnClickListener {
            updateStatus(pesananId, "DIAMBIL")
        }

        btnMulaiAntar.setOnClickListener {
            updateStatus(pesananId, "DIANTAR")
            startSendingLocation()
        }

        btnSelesai.setOnClickListener {
            updateStatus(pesananId, "SELESAI")
            stopSendingLocation()
        }
    }

    private fun updateStatus(pesananId: String, status: String) {
        db.child("pesanan").child(pesananId).child("status").setValue(status)
    }

    private fun startSendingLocation() {
        val kurirId = auth.currentUser!!.uid
        val locationRequest = LocationRequest.create().apply {
            interval = 3000 // setiap 3 detik
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val data = mapOf("lat" to loc.latitude, "lng" to loc.longitude)
                db.child("lokasi_kurir").child(kurirId).setValue(data)
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, callback, mainLooper)
    }
}