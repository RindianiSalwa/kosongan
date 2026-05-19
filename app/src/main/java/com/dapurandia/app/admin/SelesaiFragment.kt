package com.dapurandia.app.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.databinding.AdminFragmentSelesaiBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query

class SelesaiFragment : Fragment() {

    private var _binding: AdminFragmentSelesaiBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PesananAdapter
    private val db = FirebaseFirestore.getInstance()

    // 🟢 Tambahkan untuk kedua listener
    private var listenerPesananSelesai: ListenerRegistration? = null
    private var listenerAutoUpdate: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AdminFragmentSelesaiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PesananAdapter(
            listPesanan = emptyList(),
            hideButton = true
        )

        binding.rvPesananSelesai.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPesananSelesai.adapter = adapter

        autoUpdatePesananSelesaiAdmin()
        loadPesananSelesai()
    }

    private fun loadPesananSelesai() {
        listenerPesananSelesai = db.collection("pesanan")
            .whereEqualTo("status", "selesai")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->

                // 🔒 Jangan update UI kalau fragment-nya sudah hilang
                if (!isAdded || _binding == null) return@addSnapshotListener

                if (error != null || snapshot == null) return@addSnapshotListener

                val updatedList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Pesanan::class.java)?.copy(id = doc.id)
                }

                adapter.updateData(updatedList)

                binding.tvKosong.visibility =
                    if (updatedList.isEmpty()) View.VISIBLE else View.GONE

                binding.rvPesananSelesai.visibility =
                    if (updatedList.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    private fun autoUpdatePesananSelesaiAdmin() {
        val duaJamMillis = 2 * 60 * 60 * 1000

        listenerAutoUpdate = db.collection("pesanan")
            .whereEqualTo("status", "diantar")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->

                // 🔒 Hindari crash
                if (!isAdded || _binding == null) return@addSnapshotListener

                if (error != null || snapshot == null) return@addSnapshotListener

                val now = System.currentTimeMillis()

                snapshot.documents.forEach { doc ->
                    val waktuDiantar = doc.getTimestamp("waktuDiantar")?.toDate()?.time ?: return@forEach
                    if (now - waktuDiantar >= duaJamMillis) {
                        db.collection("pesanan").document(doc.id)
                            .update(
                                mapOf(
                                    "status" to "selesai",
                                    "waktuSelesai" to com.google.firebase.Timestamp.now(),
                                    "timestamp" to com.google.firebase.Timestamp.now()
                                )
                            )
                    }
                }
            }
    }

    override fun onDestroyView() {

        // 🛑 WAJIB: stop semua listener agar tidak akses binding null
        listenerPesananSelesai?.remove()
        listenerPesananSelesai = null

        listenerAutoUpdate?.remove()
        listenerAutoUpdate = null

        _binding = null
        super.onDestroyView()
    }
}
