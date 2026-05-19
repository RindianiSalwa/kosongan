package com.dapurandia.app.ui.pembeli

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.dapurandia.app.R
import com.dapurandia.app.model.Makanan
import com.dapurandia.app.utils.KeranjangManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_detail_makanan.*

class DetailMakananActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference
    private var makanan: Makanan? = null
    private var jumlah = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_makanan)

        val makananId = intent.getStringExtra("makananId") ?: return

        loadDetailMakanan(makananId)

        btnTambah.setOnClickListener {
            val stok = makanan?.stok ?: 0
            if (jumlah < stok) {
                jumlah++
                updateTampilan()
            } else {
                Toast.makeText(this, "Jumlah melebihi stok", Toast.LENGTH_SHORT).show()
            }
        }

        btnKurang.setOnClickListener {
            if (jumlah > 1) {
                jumlah--
                updateTampilan()
            }
        }

        btnTambahKeranjang.setOnClickListener {
            val m = makanan ?: return@setOnClickListener
            repeat(jumlah) {
                KeranjangManager.tambah(m)
            }
            Toast.makeText(this, "${m.nama} x$jumlah ditambahkan ke keranjang", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadDetailMakanan(makananId: String) {
        db.child("makanan").child(makananId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    makanan = snapshot.getValue(Makanan::class.java) ?: return
                    tampilkanData()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun tampilkanData() {
        val m = makanan ?: return

        tvNama.text = m.nama
        tvHarga.text = "Rp ${m.harga}"
        tvStok.text = "Stok: ${m.stok}"
        tvDeskripsi.text = m.deskripsi
        tvJumlah.text = jumlah.toString()
        tvTotal.text = "Rp ${m.harga * jumlah}"

        Glide.with(this)
            .load(m.gambarUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(ivGambar)
    }

    private fun updateTampilan() {
        val m = makanan ?: return
        tvJumlah.text = jumlah.toString()
        tvTotal.text = "Rp ${m.harga * jumlah}"
    }
}