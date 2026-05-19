package com.dapurandia.app.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.adapter.PesananAdapter
import com.example.dapurandia.model.Pesanan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminDashboardActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private lateinit var adapter: PesananAdapter
    private val listPesananAsli = mutableListOf<Pesanan>()
    private var filterAktif = "SEMUA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        setupRecyclerView()
        loadSemuaPesanan()
        setupTabFilter()

        tvLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, com.dapurandia.app.ui.auth.LoginActivity::class.java))
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = PesananAdapter(
            mutableListOf(),
            onDetailClick = { pesanan ->
                val intent = Intent(this, DetailPesananAdminActivity::class.java)
                intent.putExtra("pesananId", pesanan.id)
                startActivity(intent)
            },
            onApproveClick = { pesanan ->
                showDialogApprove(pesanan)
            }
        )
        rvPesanan.adapter = adapter
        rvPesanan.layoutManager = LinearLayoutManager(this)
    }

    private fun loadSemuaPesanan() {
        db.child("pesanan").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listPesananAsli.clear()
                for (item in snapshot.children) {
                    val pesanan = item.getValue(Pesanan::class.java)
                    if (pesanan != null) listPesananAsli.add(pesanan)
                }
                // urutkan dari terbaru
                listPesananAsli.sortByDescending { it.timestamp }
                applyFilter()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupTabFilter() {
        btnTabSemua.setOnClickListener {
            filterAktif = "SEMUA"
            applyFilter()
        }
        btnTabMenunggu.setOnClickListener {
            filterAktif = "MENUNGGU_APPROVAL"
            applyFilter()
        }
        btnTabDiproses.setOnClickListener {
            filterAktif = "DIPROSES"
            applyFilter()
        }
        btnTabSelesai.setOnClickListener {
            filterAktif = "SELESAI"
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = if (filterAktif == "SEMUA") {
            listPesananAsli.toMutableList()
        } else {
            listPesananAsli.filter { it.status == filterAktif }.toMutableList()
        }
        adapter.updateData(filtered)
    }

    private fun showDialogApprove(pesanan: Pesanan) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Approve Pesanan")
            .setMessage("Approve pesanan dari ${pesanan.pembeliNama}?")
            .setPositiveButton("Approve") { _, _ ->
                approvePesanan(pesanan.id)
            }
            .setNegativeButton("Tolak") { _, _ ->
                rejectPesanan(pesanan.id)
            }
            .setNeutralButton("Batal", null)
            .show()
    }

    private fun approvePesanan(pesananId: String) {
        db.child("pesanan").child(pesananId).child("status")
            .setValue("DIPROSES")
    }

    private fun rejectPesanan(pesananId: String) {
        db.child("pesanan").child(pesananId).child("status")
            .setValue("DITOLAK")
    }
}