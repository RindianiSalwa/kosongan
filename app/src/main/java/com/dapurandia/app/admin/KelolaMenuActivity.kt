package com.dapurandia.app.admin

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dapurandia.app.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class KelolaMenuActivity : AppCompatActivity() {

    private var originalTitle: String = "Kelola Menu"

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuAdminAdapter
    private var daftarMenuAdmin: MutableList<MenuAdmin> = mutableListOf()

    private lateinit var toolbar: Toolbar
    private lateinit var buttonTambahMenu: FloatingActionButton
    private lateinit var buttonHapusMenu: FloatingActionButton
    private lateinit var searchItem: MenuItem // Tambahkan ini di deretan lateinit

    private val firestore = FirebaseFirestore.getInstance()
    private var isSelectionMode = false

    private val tambahMenuLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_kelola_menu)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setDefaultToolbarState()
        toolbar.setTitleTextColor(Color.WHITE)

        recyclerView = findViewById(R.id.recyclerViewMenu)
        buttonTambahMenu = findViewById(R.id.buttonTambahMenu)
        buttonHapusMenu = findViewById(R.id.buttonHapusMenu)

        adapter = MenuAdminAdapter(daftarMenuAdmin)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        // klik area kosong keluar dari mode hapus
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (isSelectionMode && e.action == MotionEvent.ACTION_DOWN) {
                    val child = rv.findChildViewUnder(e.x, e.y)
                    if (child == null) {
                        exitSelectionMode()
                        return true
                    }
                }
                return false
            }
        })

        buttonTambahMenu.setOnClickListener {
            val intent = Intent(this, TambahMenuAdminActivity::class.java)
            tambahMenuLauncher.launch(intent)
        }

        buttonHapusMenu.setOnClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                adapter.setSelectionMode(true)

                buttonTambahMenu.hide()

                supportActionBar?.apply {
                    title = "Hapus Menu"
                    setDisplayHomeAsUpEnabled(true)
                    setHomeAsUpIndicator(R.drawable.ic_cancel)
                }
                toolbar.setNavigationOnClickListener {
                    exitSelectionMode()
                }

                val snackbar = Snackbar.make(
                    recyclerView,
                    "Pilih menu yang ingin dihapus",
                    Snackbar.LENGTH_SHORT
                )
                snackbar.duration = 1500
                snackbar.setBackgroundTint(Color.WHITE)
                val sbView = snackbar.view
                val textView =
                    sbView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                textView.setTextColor(ContextCompat.getColor(this, R.color.maroon_700))
                textView.textSize = 16f
                textView.textAlignment = View.TEXT_ALIGNMENT_CENTER


                val spannable = SpannableString("Pilih menu yang ingin dihapus")
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                textView.text = spannable

                snackbar.show()


            } else {
                val selectedMenus = daftarMenuAdmin.filter { it.isSelected }
                if (selectedMenus.isEmpty()) {
                    val snackbar = Snackbar.make(
                        findViewById(android.R.id.content),
                        "Tidak ada menu yang dipilih",
                        Snackbar.LENGTH_SHORT
                    )
                    snackbar.duration = 1500
                    snackbar.setBackgroundTint(Color.WHITE)
                    val sbView = snackbar.view
                    val textView =
                        sbView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                    textView.setTextColor(ContextCompat.getColor(this, R.color.maroon_700))
                    textView.textSize = 16f
                    textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

                    val spannable = SpannableString("Tidak ada menu yang dipilih")
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        0,
                        spannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    textView.text = spannable

                    snackbar.show()
                    return@setOnClickListener
                }
            AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Hapus")
                    .setMessage("Yakin ingin menghapus ${selectedMenus.size} menu?")
                    .setPositiveButton("Ya") { _, _ ->

                        val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
                        loadingDialog.progressHelper.barColor = resources.getColor(R.color.maroon_700)
                        loadingDialog.titleText = "Menghapus menu..."
                        loadingDialog.setCancelable(false)
                        loadingDialog.show()

                        var successCount = 0
                        var failCount = 0

                        for (menu in selectedMenus) {
                            firestore.collection("menus")
                                .document(menu.idMenu)
                                .delete()
                                .addOnSuccessListener {
                                    successCount++
                                    if (successCount + failCount == selectedMenus.size) {
                                        loadingDialog.dismiss()
                                        if (failCount == 0) {
                                            val successDialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                                                .setTitleText("Berhasil")
                                                .setContentText("Menu berhasil dihapus!")
                                            successDialog.show()
                                            successDialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE
                                            recyclerView.postDelayed({ successDialog.dismissWithAnimation() }, 2000)
                                        }
                                        exitSelectionMode()
                                    }
                                }
                                .addOnFailureListener {
                                    failCount++
                                    if (successCount + failCount == selectedMenus.size) {
                                        loadingDialog.dismiss()
                                        val errorDialog = SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                                            .setTitleText("Oops!")
                                            .setContentText("Gagal hapus beberapa menu. Coba lagi.")
                                        errorDialog.show()
                                        errorDialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE
                                        recyclerView.postDelayed({ errorDialog.dismissWithAnimation() }, 2000)
                                        exitSelectionMode()
                                    }
                                }
                        }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }

        listenMenusFromFirestore()
    }

    private fun setDefaultToolbarState() {
        supportActionBar?.apply {
            title = originalTitle
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
        }
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        adapter.setSelectionMode(false)
        buttonTambahMenu.show()
        if (::searchItem.isInitialized) {
            val searchView = searchItem.actionView as? SearchView
            // Kosongkan teks di searchView secara manual
            searchView?.setQuery("", false)
            // Tutup search bar
            searchItem.collapseActionView()
        }
        adapter.filterList("")
        findViewById<TextView>(R.id.tvNotFoundAdmin).visibility = View.GONE
        setDefaultToolbarState()
    }

    private fun listenMenusFromFirestore() {
        firestore.collection("menus")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Gagal mendengar update: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    daftarMenuAdmin.clear()
                    for (doc in snapshots) {
                        val idMenu = doc.getString("idMenu") ?: doc.id
                        val nama = doc.getString("nama") ?: ""
                        val harga = doc.getString("harga") ?: ""
                        val stok = doc.getLong("stok")?.toInt() ?: 0
                        val deskripsiSingkat = doc.getString("deskripsiSingkat") ?: ""
                        val deskripsiPanjang = doc.getString("deskripsi") ?: ""
                        val imageUrl = doc.getString("gambar") ?: "https://source.unsplash.com/300x200/?food"

                        daftarMenuAdmin.add(
                            MenuAdmin(
                                idMenu = idMenu,
                                nama = nama,
                                harga = harga,
                                imageUrl = imageUrl,
                                stok = stok,
                                deskripsiSingkat = deskripsiSingkat,
                                deskripsiPanjang = deskripsiPanjang
                            )
                        )
                    }
                    adapter.updateData(daftarMenuAdmin)
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_kelola_menu_toolbar, menu)

        searchItem = menu?.findItem(R.id.action_search)!!
        val searchView = searchItem?.actionView as? SearchView


        searchItem?.isVisible = true

        searchView?.queryHint = "Cari menu..."

        val searchTextId = androidx.appcompat.R.id.search_src_text
        val searchText = searchView?.findViewById<TextView>(searchTextId)
        searchText?.setTextColor(Color.WHITE)
        searchText?.setHintTextColor(Color.WHITE)

        val closeButton = searchView?.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        val magIcon = searchView?.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        closeButton?.setColorFilter(Color.WHITE)
        magIcon?.setColorFilter(Color.WHITE)

        val searchPlate = searchView?.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchPlate?.setBackgroundColor(Color.TRANSPARENT)

        searchView?.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                toolbar.navigationIcon = null
                toolbar.title = ""
            } else {
                setDefaultToolbarState()
            }
        }

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val isEmpty = adapter.filterList(newText ?: "")
                findViewById<TextView>(R.id.tvNotFoundAdmin).visibility =
                    if (isEmpty) View.VISIBLE else View.GONE
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
