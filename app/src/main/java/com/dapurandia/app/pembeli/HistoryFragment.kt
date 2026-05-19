package com.dapurandia.app.pembeli

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.admin.Pesanan
import com.dapurandia.app.databinding.PembeliFragmentHistoryBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryFragment : Fragment() {

    private var _binding: PembeliFragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PembeliPesananAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PembeliFragmentHistoryBinding.inflate(inflater, container, false)

        PembeliToolbarHelper.setup(this, binding.topAppBar, "Riwayat Pesanan")

        AlertDialog.Builder(requireContext())
            .setTitle("Informasi Riwayat Pesanan")
            .setMessage("Riwayat ini menampilkan semua pesanan Anda sebelumnya. Pesanan lebih dari 30 hari akan otomatis dihapus.")
            .setPositiveButton("OK", null)
            .show()

        adapter = PembeliPesananAdapter(
            enablePesanLagi = false,
            onPesanLagiClick = null,
            showBatal = false
        )


        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        loadHistory()

        return binding.root
    }

    private fun loadHistory() {
        val userId = auth.currentUser?.uid ?: return

        val now = System.currentTimeMillis()
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000

        db.collection("pesanan")
            .whereEqualTo("idPembeli", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val validList = mutableListOf<Pesanan>()

                for (doc in snapshot.documents) {
                    val pesanan = doc.toObject(Pesanan::class.java)?.copy(id = doc.id)
                    pesanan?.let { p ->
                        val pesananTime: Long? = when (val ts = p.timestamp) {
                            is Timestamp -> ts.toDate().time
                            is Long -> ts
                            else -> null
                        }

                        pesananTime?.let { time ->
                            if (now - time <= thirtyDaysMillis) {
                                validList.add(p)
                            } else {
                                db.collection("pesanan").document(p.id).delete()
                            }
                        }
                    }
                }

                adapter.submitList(validList)
                binding.tvKosong.visibility = if (validList.isEmpty()) View.VISIBLE else View.GONE
                binding.rvHistory.visibility = if (validList.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
