package com.dapurandia.app.pembeli

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.dapurandia.app.LandingActivity
import com.dapurandia.app.R
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PembeliFragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = binding.progressBarLoading
        val toolbar = binding.toolbarPembeli

        toolbar.title = "Dapur Andia"
        toolbar.setTitleTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.white)
        )

        toolbar.inflateMenu(R.menu.pembeli_menu_toolbar)

        val menu = toolbar.menu
        val cartMenuItem = menu.findItem(R.id.menu_cart)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.queryHint = "Cari menu..."

        // ===============================
        // SEARCH TEXT COLOR (PUTIH)
        // ===============================
        val searchText =
            searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.white)
        )
        searchText.setHintTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.white)
        )

        val closeButton =
            searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        val magIcon =
            searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        val searchPlate =
            searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)

        // 🔥 FORCE ICON COLOR (PUTIH)
        magIcon?.setColorFilter(
            ContextCompat.getColor(requireContext(), android.R.color.white),
            PorterDuff.Mode.SRC_IN
        )

        closeButton?.setColorFilter(
            ContextCompat.getColor(requireContext(), android.R.color.white),
            PorterDuff.Mode.SRC_IN
        )

        searchPlate?.setBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.transparent)
        )

        // ===============================
        // TOOLBAR BACK HANDLER
        // ===============================
        toolbar.navigationIcon = null
        toolbar.setNavigationOnClickListener {
            if (searchItem.isActionViewExpanded) {
                searchItem.collapseActionView()
            }
        }

        // ===============================
        // SEARCH EXPAND / COLLAPSE (FIX ICON WARNA)
        // ===============================
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                toolbar.title = ""
                cartMenuItem.isVisible = false

                toolbar.navigationIcon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_back)

                // 🔥 POST = KUNCI BIAR GA DIBALIKIN ABU-ABU
                toolbar.post {
                    toolbar.navigationIcon?.setTint(
                        ContextCompat.getColor(requireContext(), android.R.color.white)
                    )
                }

                // 🔥 SEARCH ICON PUTIH (LAGI)
                magIcon?.setColorFilter(
                    ContextCompat.getColor(requireContext(), android.R.color.white),
                    PorterDuff.Mode.SRC_IN
                )
                closeButton?.setColorFilter(
                    ContextCompat.getColor(requireContext(), android.R.color.white),
                    PorterDuff.Mode.SRC_IN
                )

                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                toolbar.title = "Dapur Andia"
                cartMenuItem.isVisible = true
                toolbar.navigationIcon = null
                return true
            }
        })

        // ===============================
        // SEARCH LISTENER
        // ===============================
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val isEmpty = adapter.filterList(newText ?: "")
                binding.tvNotFound.visibility =
                    if (isEmpty) View.VISIBLE else View.GONE
                return true
            }
        })

        // ===============================
        // TOOLBAR MENU ACTION
        // ===============================
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_cart -> {
                    startActivity(
                        Intent(requireContext(), KeranjangActivity::class.java)
                    )
                    true
                }

                R.id.action_logout -> {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Logout")
                        .setMessage("Apakah yakin ingin logout?")
                        .setPositiveButton("Ya") { _, _ ->
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                            val intent =
                                Intent(requireContext(), LandingActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                    true
                }

                else -> false
            }
        }

        adapter = MenuPembeliAdapter(menuList) { menu ->
            val intent =
                Intent(requireContext(), DetailMenuPembeliActivity::class.java)
            intent.putExtra("idMenu", menu.idMenu)
            intent.putExtra("namaMenu", menu.nama)
            intent.putExtra("hargaMenu", menu.harga)
            intent.putExtra("stokMenu", menu.stok)
            intent.putExtra("imageUrl", menu.imageUrl)
            intent.putExtra("deskripsiPanjang", menu.deskripsiPanjang)
            startActivity(intent)
        }

        binding.recyclerMenuPembeli.layoutManager =
            GridLayoutManager(requireContext(), 2)
        binding.recyclerMenuPembeli.adapter = adapter

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
                            deskripsiSingkat =
                                doc.getString("deskripsiSingkat") ?: "",
                            deskripsiPanjang =
                                doc.getString("deskripsi") ?: ""
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
