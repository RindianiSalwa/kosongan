package com.dapurandia.app.pembeli

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.admin.Pesanan
import com.dapurandia.app.databinding.PembeliFragmentDimasakBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class PesananDimasakFragment : Fragment() {

    private var _binding: PembeliFragmentDimasakBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PembeliPesananAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PembeliFragmentDimasakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = PembeliPesananAdapter()
        binding.recyclerViewPesanan.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPesanan.adapter = adapter
    }

    private var listenerRegistration: ListenerRegistration? = null

    private fun loadData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        listenerRegistration = db.collection("pesanan")
            .whereEqualTo("idPembeli", userId)
            .whereEqualTo("status", "dimasak")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || _binding == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Pesanan::class.java)?.copy(id = doc.id)
                }

                adapter.submitList(list)

                if (list.isEmpty()) {
                    binding.tvKosong.visibility = View.VISIBLE
                    binding.recyclerViewPesanan.visibility = View.GONE
                } else {
                    binding.tvKosong.visibility = View.GONE
                    binding.recyclerViewPesanan.visibility = View.VISIBLE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }
}
