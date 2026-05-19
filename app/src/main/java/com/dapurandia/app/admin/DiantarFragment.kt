package com.dapurandia.app.admin

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.databinding.AdminFragmentDiantarBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query

class DiantarFragment : Fragment() {

    private var _binding: AdminFragmentDiantarBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PesananAdapter
    private val db = FirebaseFirestore.getInstance()

    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AdminFragmentDiantarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PesananAdapter(
            listPesanan = emptyList(),
            hideButton = true // admin tidak bisa klik tombol di fragment ini
        )

        binding.rvPesananDiantar.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPesananDiantar.adapter = adapter

        loadPesanan()
    }

    private fun loadPesanan() {
        // 🔹 Menambahkan filter untuk menampilkan status "diantar" dan "siap_diantar"
        listenerRegistration = db.collection("pesanan")
            .whereIn("status", listOf("diantar", "siap_diantar"))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->

                // 🔒 Cegah crash jika fragment sudah tidak aktif
                if (!isAdded || _binding == null) return@addSnapshotListener

                if (error != null || snapshot == null) return@addSnapshotListener

                val updatedList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Pesanan::class.java)?.copy(id = doc.id)
                }

                adapter.updateData(updatedList)

                binding.tvKosong.visibility =
                    if (updatedList.isEmpty()) View.VISIBLE else View.GONE

                binding.rvPesananDiantar.visibility =
                    if (updatedList.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        listenerRegistration = null
        _binding = null
    }
}
