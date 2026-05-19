package com.dapurandia.app.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.databinding.AdminFragmentMenungguBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class  MenungguFragment : Fragment() {

    private var _binding: AdminFragmentMenungguBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PesananAdapter
    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AdminFragmentMenungguBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PesananAdapter(
            listPesanan = emptyList(),
            hideButton = false, 
            onStatusUpdated = {  }
        )

        binding.rvPesananMenunggu.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPesananMenunggu.adapter = adapter

        loadPesananMenunggu()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvPesananMenunggu.visibility = if (show) View.GONE else View.VISIBLE
        binding.tvKosong.visibility = View.GONE
    }

    private fun loadPesananMenunggu() {
        if (!isAdded || _binding == null) return

        showLoading(true)

        listenerRegistration?.remove()

        listenerRegistration = db.collection("pesanan")
            .whereEqualTo("status", "Menunggu")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded || _binding == null) return@addSnapshotListener
                if (error != null) {
                    showLoading(false)
                    Log.e("MenungguFragment", "Listen failed: ${error.message}")
                    return@addSnapshotListener
                }

                val updatedList = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Pesanan::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } ?: emptyList()

                showLoading(false)
                adapter.updateData(updatedList)

                binding.tvKosong.visibility =
                    if (updatedList.isEmpty()) View.VISIBLE else View.GONE
                binding.rvPesananMenunggu.visibility =
                    if (updatedList.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }
}
