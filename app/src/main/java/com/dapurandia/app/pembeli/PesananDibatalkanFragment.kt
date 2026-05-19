package com.dapurandia.app.pembeli

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.admin.Pesanan
import com.dapurandia.app.databinding.PembeliFragmentPesananMenungguBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.content.Intent
import cn.pedant.SweetAlert.SweetAlertDialog



class PesananDibatalkanFragment : Fragment() {

    private var _binding: PembeliFragmentPesananMenungguBinding? = null
    private val binding get() = _binding
    private lateinit var adapter: PembeliPesananAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PembeliFragmentPesananMenungguBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        // Enable tombol "Pesan Lagi" dengan logika sama seperti di fragment Selesai
        adapter = PembeliPesananAdapter(
            enablePesanLagi = true,
            onPesanLagiClick = { pesanan -> pesanLagi(pesanan) }
        )
        binding?.recyclerViewPesanan?.layoutManager =
            LinearLayoutManager(requireContext())

        binding?.recyclerViewPesanan?.adapter = adapter

    }

    private fun loadData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("pesanan")
            .whereEqualTo("idPembeli", userId)
            .whereEqualTo("status", "Dibatalkan")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (_binding == null || !isAdded) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Pesanan::class.java)?.copy(id = doc.id)
                }

                adapter.submitList(list)

                _binding?.let { bind ->
                    bind.tvKosong.visibility =
                        if (list.isEmpty()) View.VISIBLE else View.GONE

                    bind.recyclerViewPesanan.visibility =
                        if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
    }

    private fun pesanLagi(pesanan: Pesanan) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val keranjangRef = db.collection("keranjang").document(userId).collection("items")

        val menuNames = pesanan.items.map { it.nama }.distinct()

        db.collection("menus")
            .whereIn("nama", menuNames)
            .get()
            .addOnSuccessListener { menusSnapshot ->

                val stokMap = menusSnapshot.associate { doc ->
                    val nama = doc.getString("nama") ?: ""
                    val stok = doc.getLong("stok")?.toInt() ?: 0
                    nama to stok
                }

                keranjangRef.get().addOnSuccessListener { snapshot ->
                    val batch = db.batch()
                    val existingItems = snapshot.documents.associateBy { it.getString("namaMenu") }

                    var adaStokHabis = false

                    pesanan.items.forEach { item ->
                        val stokTersedia = stokMap[item.nama] ?: 0

                        // 🚨 STOK HABIS
                        if (stokTersedia <= 0) {
                            adaStokHabis = true
                            return@forEach
                        }

                        val existing = existingItems[item.nama]

                        if (existing != null) {
                            val jumlahSekarang = existing.getLong("jumlah")?.toInt() ?: 0
                            batch.update(existing.reference, mapOf(
                                "jumlah" to (jumlahSekarang + 1),
                                "stok" to stokTersedia,
                                "isChecked" to true
                            ))
                        } else {
                            val newItemRef = keranjangRef.document()
                            batch.set(newItemRef, mapOf(
                                "namaMenu" to item.nama,
                                "harga" to item.harga,
                                "jumlah" to 1,
                                "imageUrl" to item.gambar,
                                "docId" to newItemRef.id,
                                "stok" to stokTersedia,
                                "isChecked" to true
                            ))
                        }
                    }

                    if (adaStokHabis) {
                        Toast.makeText(
                            requireContext(),
                            "Beberapa menu stoknya habis",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@addOnSuccessListener
                    }
                    batch.commit()
                        .addOnSuccessListener {

                            val dialog = SweetAlertDialog(
                                requireContext(),
                                SweetAlertDialog.SUCCESS_TYPE
                            )
                                .setTitleText("Berhasil")
                                .setContentText("Menu ditambahkan ke keranjang")
                                .hideConfirmButton()

                            dialog.show()

                            android.os.Handler().postDelayed({
                                dialog.dismissWithAnimation()

                                val intent = Intent(requireContext(), KeranjangActivity::class.java)
                                startActivity(intent)

                            }, 1200) // 1.2 detik biar kebaca dulu
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal mengambil stok menu", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
