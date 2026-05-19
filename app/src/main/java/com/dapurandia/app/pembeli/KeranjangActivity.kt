package com.dapurandia.app.pembeli

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapurandia.app.databinding.PembeliActivityKeranjangBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.appcompat.widget.Toolbar
import com.dapurandia.app.R

class KeranjangActivity : AppCompatActivity() {

    private var isLoading = false
    private lateinit var binding: PembeliActivityKeranjangBinding

    private val listKeranjang = ArrayList<KeranjangItem>()
    private lateinit var adapter: KeranjangAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var cartListener: ListenerRegistration? = null
    private var menusListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = PembeliActivityKeranjangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarKeranjang.setTitleTextColor(
            ContextCompat.getColor(this, android.R.color.white)
        )
        setSupportActionBar(binding.toolbarKeranjang)
        supportActionBar?.title = "Keranjang"
        val toolbarKeranjang = findViewById<Toolbar>(R.id.toolbarKeranjang)
        setSupportActionBar(toolbarKeranjang)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbarKeranjang.navigationIcon?.setTint(
            ContextCompat.getColor(this, android.R.color.white)
        )

        adapter = KeranjangAdapter(listKeranjang) { selectedItems ->
            updateTotalHarga(selectedItems)
        }

        binding.recyclerViewKeranjang.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewKeranjang.adapter = adapter

        // 🔒 STATE AWAL - CEGAH KEDIP
        binding.cardCheckoutFooter.visibility = View.GONE
        binding.recyclerViewKeranjang.visibility = View.GONE
        binding.textKosong.visibility = View.GONE
        binding.progressBarKeranjang.visibility = View.VISIBLE
        setCheckoutButtonEnabled(false)

        binding.buttonCheckout.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            db.collection("pembeli").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val nama = document.getString("nama") ?: ""
                        val noHp = document.getString("no_hp") ?: ""

                        val selectedItems = adapter.getSelectedItems()

                        if (selectedItems.isEmpty()) {
                            Toast.makeText(
                                this,
                                "Pilih menu yang ingin dipesan",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@addOnSuccessListener
                        }

                        val intent = Intent(this, CheckoutActivity::class.java).apply {
                            putExtra("NAMA_PEMBELI", nama)
                            putExtra("NO_HP_PEMBELI", noHp)
                            putExtra("ITEMS_CHECKOUT", ArrayList(selectedItems))
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Data profil tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mengambil data profil", Toast.LENGTH_SHORT).show()
                }
        }

        loadDataKeranjang()
    }

    private fun loadDataKeranjang() {
        if (isLoading) return
        isLoading = true

        val userId = auth.currentUser?.uid ?: return

        binding.progressBarKeranjang.visibility = View.VISIBLE
        binding.recyclerViewKeranjang.visibility = View.GONE
        binding.textKosong.visibility = View.GONE

        cartListener?.remove()
        menusListener?.remove()

        cartListener = db.collection("keranjang").document(userId).collection("items")
            .addSnapshotListener { cartSnapshot, cartError ->

                if (cartError != null || cartSnapshot == null) {
                    Toast.makeText(this, "Gagal memuat keranjang", Toast.LENGTH_SHORT).show()
                    updateVisibilityState(true)
                    binding.progressBarKeranjang.visibility = View.GONE
                    isLoading = false
                    return@addSnapshotListener
                }

                val tempList = cartSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(KeranjangItem::class.java)?.apply {
                        docId = doc.id
                        isChecked = false
                    }
                }

                if (tempList.isEmpty()) {
                    listKeranjang.clear()
                    adapter.notifyDataSetChanged()
                    updateVisibilityState(true)
                    updateTotalHarga(emptyList())
                    binding.progressBarKeranjang.visibility = View.GONE
                    isLoading = false
                    return@addSnapshotListener
                }

                val menuNames = tempList.map { it.namaMenu }.distinct()

                menusListener?.remove()
                menusListener = db.collection("menus")
                    .whereIn("nama", menuNames)
                    .addSnapshotListener { menusSnapshot, _ ->
                        if (menusSnapshot == null) return@addSnapshotListener

                        val stokMap = menusSnapshot.associate { menuDoc ->
                            val nama = menuDoc.getString("nama") ?: ""
                            val stok = menuDoc.getLong("stok")?.toInt() ?: 0
                            nama to stok
                        }

                        tempList.forEach { item ->
                            item.stok = stokMap[item.namaMenu] ?: 0
                        }

                        listKeranjang.clear()
                        listKeranjang.addAll(tempList)
                        adapter.notifyDataSetChanged()
                        adapter.clearSelection()

                        updateVisibilityState(listKeranjang.isEmpty())
                        updateTotalHarga(adapter.getSelectedItems())

                        binding.progressBarKeranjang.visibility = View.GONE
                        isLoading = false
                    }
            }
    }

    private fun updateTotalHarga(selectedItems: List<KeranjangItem>) {
        val total = selectedItems.sumOf {
            it.jumlah * (it.harga.toIntOrNull() ?: 0)
        }

        val formatter = java.text.NumberFormat.getCurrencyInstance(
            java.util.Locale("id", "ID")
        ).apply {
            maximumFractionDigits = 0
        }

        binding.textTotalHarga.text = "Total: ${formatter.format(total)}"
        binding.textJumlahDipilih.text =
            "${selectedItems.sumOf { it.jumlah }} item"

        setCheckoutButtonEnabled(selectedItems.isNotEmpty())
    }

    fun hapusItem(item: KeranjangItem, position: Int) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("keranjang")
            .document(userId)
            .collection("items")
            .document(item.docId)
            .delete()
            .addOnSuccessListener {
                listKeranjang.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateTotalHarga(adapter.getSelectedItems())
                updateVisibilityState(listKeranjang.isEmpty())
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menghapus item", Toast.LENGTH_SHORT).show()
            }
    }

    fun updateVisibilityState(isEmpty: Boolean) {
        binding.textKosong.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewKeranjang.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.cardCheckoutFooter.visibility = if (isEmpty) View.GONE else View.VISIBLE

        if (isEmpty) {
            setCheckoutButtonEnabled(false)
        }
    }

    private fun setCheckoutButtonEnabled(enabled: Boolean) {
        binding.buttonCheckout.isEnabled = enabled
        binding.buttonCheckout.alpha = if (enabled) 1f else 0.5f
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }


    override fun onDestroy() {
        cartListener?.remove()
        menusListener?.remove()
        super.onDestroy()
    }
}
