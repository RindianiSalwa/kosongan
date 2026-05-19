package com.dapurandia.app.ui.pembeli

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dapurandia.app.R
import com.dapurandia.app.model.Pesanan
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_status_pesanan.*

class StatusPesananActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference
    private var kurirId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status_pesanan)

        val pesananId = intent.getStringExtra("pesananId") ?: return

        listenStatusPesanan(pesananId)

        btnTracking.setOnClickListener {
            val intent = Intent(this, TrackingActivity::class.java)
            intent.putExtra("kurirId", kurirId)
            startActivity(intent)
        }
    }

    private fun listenStatusPesanan(pesananId: String) {
        db.child("pesanan").child(pesananId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pesanan = snapshot.getValue(Pesanan::class.java) ?: return

                    tvIdPesanan.text = pesanan.id
                    tvStatus.text = pesanan.status
                    tvAlamat.text = pesanan.alamat
                    tvTotal.text = "Rp ${pesanan.totalHarga}"

                    kurirId = pesanan.kurirId

                    // tampilkan tombol tracking kalau sudah diantar
                    if (pesanan.status == "DIANTAR") {
                        btnTracking.visibility = View.VISIBLE
                    } else {
                        btnTracking.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}