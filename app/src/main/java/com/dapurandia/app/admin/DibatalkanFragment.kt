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
import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat

class DibatalkanFragment : Fragment() {

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

        binding.tvKosong.text = "Tidak ada menu yang dibatalkan"
        binding.tvKosong.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        binding.tvKosong.setTypeface(null, Typeface.BOLD)
        binding.tvKosong.gravity = Gravity.CENTER

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PesananAdapter(
            listPesanan = emptyList(),
            hideButton = true
        )

        binding.rvPesananMenunggu.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPesananMenunggu.adapter = adapter

        loadPesananDibatalkan()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvPesananMenunggu.visibility = if (show) View.GONE else View.VISIBLE
        binding.tvKosong.visibility = View.GONE
    }

    private fun loadPesananDibatalkan() {
        if (!isAdded || _binding == null) return

        showLoading(true)

        listenerRegistration?.remove()

        listenerRegistration = db.collection("pesanan")
            .whereEqualTo("status", "Dibatalkan")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded || _binding == null) return@addSnapshotListener
                if (error != null) {
                    showLoading(false)
                    Log.e("DibatalkanFragment", "Listen failed: ${error.message}")
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
