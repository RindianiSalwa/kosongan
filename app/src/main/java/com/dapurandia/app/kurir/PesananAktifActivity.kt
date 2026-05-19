package com.dapurandia.app.kurir

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dapurandia.app.R
import com.dapurandia.app.admin.Pesanan
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar

class PesananAktifActivity : AppCompatActivity() {

    private lateinit var rvPesananAktif: RecyclerView
    private lateinit var adapter: KurirPesananAktifAdapter
    private val pesananList = mutableListOf<Pesanan>()
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null
    private val LOCATION_REQUEST_CODE = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.kurir_activity_pesanan_aktif)

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
        }
        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarPesananAktif)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pesanan Aktif"
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Untuk handle padding system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.emptyLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // RecyclerView
        rvPesananAktif = findViewById(R.id.recyclerViewPesanan)
        rvPesananAktif.layoutManager = LinearLayoutManager(this)
        adapter = KurirPesananAktifAdapter(pesananList)
        rvPesananAktif.adapter = adapter

        fetchPesananAktif()
    }

    private fun fetchPesananAktif() {
        listener = db.collection("pesanan")
            .whereIn("status", listOf("siap_diantar", "diantar"))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) return@addSnapshotListener

                pesananList.clear()
                snapshot?.documents?.forEach { doc ->
                    val pesanan = doc.toObject(Pesanan::class.java)?.copy(id = doc.id)
                    pesanan?.let { pesananList.add(it) }
                }

                adapter.notifyDataSetChanged()

                findViewById<View>(R.id.tvKosong)?.visibility =
                    if (pesananList.isEmpty()) View.VISIBLE else View.GONE
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Izin lokasi diberikan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE
        )
    }




    private fun enableEdgeToEdge() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
}
