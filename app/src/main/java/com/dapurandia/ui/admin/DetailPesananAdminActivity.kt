package com.dapurandia.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dapurandia.app.R
import com.dapurandia.app.model.Pesanan
import com.dapurandia.app.model.User
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_detail_pesanan_admin.*

class DetailPesananAdminActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference
    private val listKurir = mutableListOf<User>()
    private var pesananId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_pesanan_admin)

        pesananId = intent.getStringExtra("pesananId") ?: return

        ivBack.setOnClickListener { finish() }

        loadDetailPesanan()
        loadDaftarKurir()

        btnAssignKurir.setOnClickListener {
            assignKurir()
        }
    }

    private fun loadDetailPesanan() {
        db.child("pesanan").child(pesananId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pesanan = snapshot.getValue(Pesanan::class.java) ?: return

                    tvIdPesanan.text = "ID: #${pesanan.id.takeLast(6).uppercase()}"
                    tvNamaPembeli.text = "Pembeli: ${pesanan.pembeliNama}"
                    tvAlamat.text = "Alamat: ${pesanan.alamat}"
                    tvCatatan.text = "Catatan: ${pesanan.catatan.ifEmpty { "-" }}"
                    tvStatus.text = "Status: ${pesanan.status}"
                    tvTotal.text = "Total: Rp ${pesanan.totalHarga}"

                    // tampilkan item pesanan
                    val sb = StringBuilder()
                    for ((_, item) in pesanan.items) {
                        sb.append("• ${item.nama} x${item.jumlah} — Rp ${item.harga * item.jumlah}\n")
                    }
                    tvItemPesanan.text = sb.toString().trimEnd()

                    // tampilkan assign kurir hanya kalau status DIPROSES
                    if (pesanan.status == "DIPROSES") {
                        layoutAssignKurir.visibility = View.VISIBLE
                    } else {
                        layoutAssignKurir.visibility = View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadDaftarKurir() {
        db.child("users").orderByChild("role").equalTo("kurir")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listKurir.clear()
                    for (item in snapshot.children) {
                        val kurir = item.getValue(User::class.java)
                        if (kurir != null) listKurir.add(kurir)
                    }

                    val namaKurir = listKurir.map { it.nama }
                    val spinnerAdapter = ArrayAdapter(
                        this@DetailPesananAdminActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        namaKurir
                    )
                    spinnerKurir.adapter = spinnerAdapter
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun assignKurir() {
        val indexTerpilih = spinnerKurir.selectedItemPosition
        if (listKurir.isEmpty()) {
            Toast.makeText(this, "Tidak ada kurir tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val kurir = listKurir[indexTerpilih]

        val update = mapOf(
            "kurirId" to kurir.uid,
            "status" to "MENUNGGU_KURIR"
        )

        db.child("pesanan").child(pesananId).updateChildren(update)
            .addOnSuccessListener {
                Toast.makeText(this, "Kurir ${kurir.nama} berhasil di-assign", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal assign kurir", Toast.LENGTH_SHORT).show()
            }
    }
}