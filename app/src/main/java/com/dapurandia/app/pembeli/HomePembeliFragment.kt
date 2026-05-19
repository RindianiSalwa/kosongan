package com.dapurandia.app.pembeli

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.dapurandia.app.databinding.PembeliFragmentHomeBinding
import com.google.firebase.firestore.FirebaseFirestore

class HomePembeliFragment : Fragment() {

    private var _binding: PembeliFragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressBar: ProgressBar
    private val db = FirebaseFirestore.getInstance()
    private val menuList = mutableListOf<MenuPembeli>()
    private lateinit var adapter: MenuPembeliAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PembeliFragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = binding.progressBarLoading

        PembeliToolbarHelper.setup(this, binding.toolbarPembeli, "Beranda") {
            binding.searchMenu.requestFocus()
            val inputMethodManager = requireContext()
                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(binding.searchMenu, InputMethodManager.SHOW_IMPLICIT)
        }

        adapter = MenuPembeliAdapter(menuList) { menu ->
            val intent = Intent(requireContext(), DetailMenuPembeliActivity::class.java)
            intent.putExtra("idMenu", menu.idMenu)
            intent.putExtra("namaMenu", menu.nama)
            intent.putExtra("hargaMenu", menu.harga)
            intent.putExtra("stokMenu", menu.stok)
            intent.putExtra("imageUrl", menu.imageUrl)
            intent.putExtra("deskripsiPanjang", menu.deskripsiPanjang)
            startActivity(intent)
        }

        binding.recyclerMenuPembeli.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerMenuPembeli.adapter = adapter

        binding.searchMenu.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val isEmpty = adapter.filterList(s?.toString().orEmpty())
                binding.tvNotFound.visibility = if (isEmpty) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        loadData()
    }

    private fun loadData() {
        progressBar.visibility = View.VISIBLE

        db.collection("menus")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Gagal mendengar update: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    progressBar.visibility = View.GONE
                    val newList = mutableListOf<MenuPembeli>()

                    snapshots.forEach { doc ->
                        val item = MenuPembeli(
                            idMenu = doc.id,
                            nama = doc.getString("nama") ?: "",
                            harga = doc.getString("harga") ?: "",
                            imageUrl = doc.getString("gambar") ?: "",
                            stok = doc.getLong("stok")?.toInt() ?: 0,
                            deskripsiSingkat = doc.getString("deskripsiSingkat") ?: "",
                            deskripsiPanjang = doc.getString("deskripsi") ?: ""
                        )
                        newList.add(item)
                    }

                    menuList.clear()
                    menuList.addAll(newList)
                    adapter.updateData(menuList)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
