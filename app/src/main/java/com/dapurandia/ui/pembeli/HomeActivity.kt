package com.dapurandia.app.ui.pembeli

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.R
import com.dapurandia.app.adapter.MakananAdapter
import com.dapurandia.app.model.Makanan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private lateinit var adapter: MakananAdapter
    private val listMakanan = mutableListOf<Makanan>()
    private val listMakananAsli = mutableListOf<Makanan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupNamaPengguna()
        setupRecyclerView()
        setupSearch()
        loadMakanan()

        ivKeranjang.setOnClickListener {
            startActivity(Intent(this, KeranjangActivity::class.java))
        }
    }

    private fun setupNamaPengguna() {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("nama").get()
            .addOnSuccessListener { snapshot ->
                val nama = snapshot.value.toString()
                tvSalam.text = "Halo, $nama!"
            }
    }

    private fun setupRecyclerView() {
        adapter = MakananAdapter(
            listMakanan,
            onItemClick = { makanan ->
                val intent = Intent(this, DetailMakananActivity::class.java)
                intent.putExtra("makananId", makanan.id)
                startActivity(intent)
            },
            onTambahClick = { makanan ->
                tambahKeKeranjang(makanan)
            }
        )
        rvMakanan.adapter = adapter
        rvMakanan.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString(), listMakananAsli)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadMakanan() {
        db.child("makanan").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listMakanan.clear()
                listMakananAsli.clear()
                for (item in snapshot.children) {
                    val makanan = item.getValue(Makanan::class.java)
                    if (makanan != null && makanan.stok > 0) {
                        listMakanan.add(makanan)
                        listMakananAsli.add(makanan)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun tambahKeKeranjang(makanan: Makanan) {
        // simpan ke list keranjang sementara di memory
        KeranjangManager.tambah(makanan)
        android.widget.Toast.makeText(this, "${makanan.nama} ditambahkan ke keranjang", android.widget.Toast.LENGTH_SHORT).show()
    }
}