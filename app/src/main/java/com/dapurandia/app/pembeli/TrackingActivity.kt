package com.dapurandia.app.pembeli

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dapurandia.app.R
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.PolyUtil
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class TrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var markerKurir: Marker? = null
    private var markerTujuan: Marker? = null
    private val db = FirebaseFirestore.getInstance()
    private var routePolyline: Polyline? = null
    private var grayPolyline: Polyline? = null
    private var routePoints: List<LatLng> = emptyList()

    private lateinit var tvJarakPembeli: TextView
    private lateinit var tvWaktuPembeli: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pembeli_activity_tracking)
        tvJarakPembeli = findViewById(R.id.tvJarakPembeli)
        tvWaktuPembeli = findViewById(R.id.tvWaktuPembeli)

        findViewById<ImageView>(R.id.btnBackTracking).setOnClickListener { finish() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapTracking) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.isBuildingsEnabled = false

        val idPesanan = intent.getStringExtra("idPesanan") ?: return
        val destLat = intent.getDoubleExtra("destLat", 0.0)
        val destLng = intent.getDoubleExtra("destLng", 0.0)

        val lokasiTujuan = LatLng(destLat, destLng)
        markerTujuan = mMap.addMarker(MarkerOptions()
            .position(lokasiTujuan)
            .title("Lokasi Saya")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lokasiTujuan, 15f))

        mulaiTrackingKurir(idPesanan)
    }

    private fun mulaiTrackingKurir(idPesanan: String) {
        db.collection("pesanan").document(idPesanan)
            .addSnapshotListener { snapshot, e ->
                if (e != null || isFinishing || isDestroyed) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    // Update Estimasi UI
                    tvJarakPembeli.text = snapshot.getString("estimasiJarak") ?: "-- km"
                    tvWaktuPembeli.text = snapshot.getString("estimasiWaktu") ?: "-- menit"

                    val status = snapshot.getString("status")

                    if (status == "diantar") {
                        // 🔥 SINKRONISASI NAMA FIELD: latKurir & lngKurir
                        val lat = snapshot.getDouble("latKurir")
                        val lng = snapshot.getDouble("lngKurir")

                        if (lat != null && lng != null) {
                            val posisiKurir = LatLng(lat, lng)
                            updateMarkerKurir(posisiKurir, snapshot)
                        }
                    }
                    else if (status == "selesai") {
                        Toast.makeText(applicationContext, "Pesanan sampai!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    else if (status == "siap_diantar") {
                        // Jika kurir membatalkan navigasi di tengah jalan
                        Toast.makeText(applicationContext, "Kurir menjeda pengantaran", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
    }

    private fun updateMarkerKurir(posisiBaru: LatLng, snapshot: com.google.firebase.firestore.DocumentSnapshot) {
        val bearing = snapshot.getDouble("bearing")?.toFloat() ?: 0f

        if (routePoints.isEmpty()) {
            loadRoute(posisiBaru, markerTujuan!!.position)
        }

        if (markerKurir == null) {
            markerKurir = mMap.addMarker(MarkerOptions()
                .position(posisiBaru)
                .rotation(bearing)
                .flat(true)
                .anchor(0.5f, 0.5f)
                .icon(bitmapDescriptorFromVector(R.drawable.ic_kurir_motor)))

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posisiBaru, 17f))
        } else {
            // Animasi marker berpindah posisi secara halus
            animateMarker(markerKurir!!, posisiBaru, bearing)
        }

        // Kamera mengikuti kurir dengan posisi miring (Tilt)
        val cameraPosition = CameraPosition.Builder()
            .target(posisiBaru)
            .zoom(18.0f)
            .tilt(45f)
            .bearing(bearing)
            .build()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null)

        updatePolylinesPembeli(posisiBaru)
    }

    // Fungsi tambahan agar marker tidak "teleportasi" tapi bergeser halus
    private fun animateMarker(marker: Marker, toPosition: LatLng, bearing: Float) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val start = System.currentTimeMillis()
        val duration: Long = 1000
        val interpolator = android.view.animation.LinearInterpolator()
        val startLatLng = marker.position

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - start
                val t = interpolator.getInterpolation(elapsed.toFloat() / duration)
                val lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude
                val lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude

                marker.position = LatLng(lat, lng)
                marker.rotation = bearing

                if (t < 1.0) {
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    private fun bitmapDescriptorFromVector(vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = androidx.core.content.ContextCompat.getDrawable(this, vectorResId)
        val targetWidth = 120 // Ukuran dikecilkan sedikit agar proporsional
        val targetHeight = 120
        vectorDrawable?.setBounds(0, 0, targetWidth, targetHeight)
        val bitmap = android.graphics.Bitmap.createBitmap(targetWidth, targetHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun loadRoute(origin: LatLng, dest: LatLng) {
        val apiKey = getString(R.string.google_directions_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${dest.latitude},${dest.longitude}&mode=driving&key=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { json ->
                    val obj = JSONObject(json)
                    if (obj.getString("status") == "OK") {
                        val polyline = obj.getJSONArray("routes").getJSONObject(0).getJSONObject("overview_polyline").getString("points")
                        routePoints = PolyUtil.decode(polyline)
                        runOnUiThread {
                            routePolyline?.remove()
                            routePolyline = mMap.addPolyline(PolylineOptions().addAll(routePoints).width(12f).color(android.graphics.Color.BLUE))
                        }
                    }
                }
            }
        })
    }

    private fun updatePolylinesPembeli(currentPos: LatLng) {
        if (routePoints.isEmpty()) return
        val closestIndex = routePoints.withIndex().minByOrNull { distanceBetween(it.value, currentPos) }?.index ?: return

        val done = routePoints.subList(0, closestIndex + 1)
        val remain = routePoints.subList(closestIndex, routePoints.size)

        if (grayPolyline == null) {
            grayPolyline = mMap.addPolyline(PolylineOptions().addAll(done).width(12f).color(android.graphics.Color.LTGRAY))
        } else {
            grayPolyline?.points = done
        }
        routePolyline?.points = remain
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val result = FloatArray(1)
        android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result[0]
    }
}