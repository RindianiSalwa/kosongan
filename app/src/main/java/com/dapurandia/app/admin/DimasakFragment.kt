package com.dapurandia.app.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.databinding.AdminFragmentDimasakBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query

class DimasakFragment : Fragment() {

    private var _binding: AdminFragmentDimasakBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PesananAdapter
    private val db = FirebaseFirestore.getInstance()
    private val pesananList = mutableListOf<Pesanan>()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AdminFragmentDimasakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PesananAdapter(
            listPesanan = pesananList,
            showAntarButton = true,
            onStatusUpdated = { loadPesananDimasak() }
        )

        binding.rvPesananDimasak.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPesananDimasak.adapter = adapter

        loadPesananDimasak()
    }

    private fun loadPesananDimasak() {
        listenerRegistration?.remove()
        listenerRegistration = db.collection("pesanan")
            .whereEqualTo("status", "dimasak")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null || _binding == null) return@addSnapshotListener

                val updatedList = mutableListOf<Pesanan>()
                for (doc in snapshot.documents) {
                    val pesanan = doc.toObject(Pesanan::class.java)
                    pesanan?.let {
                        updatedList.add(it.copy(id = doc.id))
                    }
                }

                adapter.updateData(updatedList)

                binding.tvKosong.visibility = if (updatedList.isEmpty()) View.VISIBLE else View.GONE
                binding.rvPesananDimasak.visibility = if (updatedList.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    override fun onDestroyView() {
        listenerRegistration?.remove()
        listenerRegistration = null
        _binding = null
        super.onDestroyView()
    }
}
