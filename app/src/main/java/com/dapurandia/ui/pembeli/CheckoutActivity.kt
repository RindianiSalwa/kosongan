package com.dapurandia.app.ui.pembeli

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dapurandia.app.R
import com.dapurandia.app.model.ItemPesanan
import com.dapurandia.app.model.Pesanan
import com.dapurandia.app.utils.KeranjangManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_checkout.*

class CheckoutActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        ivBack.setOnClickListener { finish() }

        tampilkanRingkasan()

        btnPesan.setOnClickListener {
            buatPesanan()
        }
    }

    private fun tampilkanRingkasan() {
        val keranjang = KeranjangManager.getKeranjang()
        val sb = StringBuilder()

        for ((_, pair) in keranjang) {
            val (makanan, jumlah) = pair
            sb.append("${makanan.nama} x$jumlah — Rp ${makanan.harga * jumlah}\n")
        }

        tvRingkasan.text = sb.toString().trimEnd()
        tvTotal.text = "Rp ${KeranjangManager.totalHarga()}"
    }

    private fun buatPesanan() {
        val alamat = etAlamat.text.toString().trim()
        val catatan = etCatatan.text.toString().trim()

        if (alamat.isEmpty()) {
            etAlamat.error = "Alamat wajib diisi"
            return
        }

        btnPesan.isEnabled = false
        btnPesan.text = "Memproses..."

        val uid = auth.currentUser!!.uid

        // ambil nama pembeli dulu
        db.child("users").child(uid).child("nama").get()
            .addOnSuccessListener { snapshot ->
                val namaPembeli = snapshot.value.toString()

                // konversi keranjang ke map ItemPesanan
                val items = mutableMapOf<String, ItemPesanan>()
                for ((makananId, pair) in KeranjangManager.getKeranjang()) {
                    val (makanan, jumlah) = pair
                    items[makananId] = ItemPesanan(
                        makananId = makananId,
                        nama = makanan.nama,
                        harga = makanan.harga,
                        jumlah = jumlah
                    )
                }

                val pesananId = db.child("pesanan").push().key!!

                val pesanan = Pesanan(
                    id = pesananId,
                    pembeliId = uid,
                    pembeliNama = namaPembeli,
                    alamat = alamat,
                    catatan = catatan,
                    items = items,
                    totalHarga = KeranjangManager.totalHarga(),
                    status = "MENUNGGU_APPROVAL",
                    timestamp = System.currentTimeMillis()
                )

                db.child("pesanan").child(pesananId).setValue(pesanan)
                    .addOnSuccessListener {
                        KeranjangManager.kosongkan()
                        Toast.makeText(this, "Pesanan berhasil dibuat!", Toast.LENGTH_SHORT).show()

                        // ke halaman status pesanan
                        val intent = Intent(this, StatusPesananActivity::class.java)
                        intent.putExtra("pesananId", pesananId)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener {
                        btnPesan.isEnabled = true
                        btnPesan.text = "Buat Pesanan"
                        Toast.makeText(this, "Gagal membuat pesanan", Toast.LENGTH_SHORT).show()
                    }
            }
    }
}