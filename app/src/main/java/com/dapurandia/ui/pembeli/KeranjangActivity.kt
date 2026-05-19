package com.dapurandia.app.ui.pembeli

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.R
import com.dapurandia.app.adapter.KeranjangAdapter
import com.dapurandia.app.utils.KeranjangManager
import kotlinx.android.synthetic.main.activity_keranjang.*

class KeranjangActivity : AppCompatActivity() {

    private lateinit var adapter: KeranjangAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keranjang)

        ivBack.setOnClickListener { finish() }

        setupRecyclerView()
        updateTotal()

        btnCheckout.setOnClickListener {
            if (KeranjangManager.getKeranjang().isEmpty()) {
                Toast.makeText(this, "Keranjang masih kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, CheckoutActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        val items = KeranjangManager.getKeranjang().values.toMutableList()
        adapter = KeranjangAdapter(items) {
            updateTotal()
        }
        rvKeranjang.adapter = adapter
        rvKeranjang.layoutManager = LinearLayoutManager(this)
    }

    private fun updateTotal() {
        tvTotalHarga.text = "Rp ${KeranjangManager.totalHarga()}"
    }

    override fun onResume() {
        super.onResume()
        adapter.refresh()
        updateTotal()
    }
}