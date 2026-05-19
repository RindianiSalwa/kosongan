package com.dapurandia.app.pembeli

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.admin.Pesanan
import com.dapurandia.app.databinding.PembeliFragmentPesananDiantarBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query

class PesananDiantarFragment : Fragment() {

    private var _binding: PembeliFragmentPesananDiantarBinding? = null
    private val binding get() = _binding!!

    // ✅ PAKAI ADAPTER PEMBELI
    private lateinit var adapter: PembeliPesananAdapter

    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PembeliFragmentPesananDiantarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PembeliPesananAdapter(
            showBatal = true // 🔥 supaya tombol Pesanan Selesai muncul saat status = diantar
        )

        binding.recyclerViewPesanan.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPesanan.adapter = adapter

        loadPesanan()
    }

    private fun loadPesanan() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        listenerRegistration = db.collection("pesanan")
            .whereEqualTo("idPembeli", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->

                if (!isAdded || _binding == null) return@addSnapshotListener
                if (error != null || snapshot == null) return@addSnapshotListener

                val filteredList = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Pesanan::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "PESANAN_PARSE_ERROR",
                            "Dokumen rusak dilewati: ${doc.id}",
                            e
                        )
                        null
                    }
                }.filter {
                    it.status.equals("siap_diantar", ignoreCase = true) ||
                            it.status.equals("diantar", ignoreCase = true)
                }



                adapter.submitList(filteredList)

                binding.tvKosong.visibility =
                    if (filteredList.isEmpty()) View.VISIBLE else View.GONE

                binding.recyclerViewPesanan.visibility =
                    if (filteredList.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        listenerRegistration = null
        _binding = null
    }
}
