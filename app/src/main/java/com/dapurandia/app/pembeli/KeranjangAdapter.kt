package com.dapurandia.app.pembeli

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dapurandia.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class KeranjangAdapter(
    private val list: MutableList<KeranjangItem>,
    private val onSelectionChanged: (List<KeranjangItem>) -> Unit
) : RecyclerView.Adapter<KeranjangAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nama = view.findViewById<TextView>(R.id.textNama)
        val harga = view.findViewById<TextView>(R.id.textHarga)
        val jumlah = view.findViewById<TextView>(R.id.textJumlah)
        val image = view.findViewById<ImageView>(R.id.imageMenu)
        val checkBox = view.findViewById<CheckBox>(R.id.checkBoxPilih)
        val buttonTambah = view.findViewById<Button>(R.id.buttonTambah)
        val buttonKurang = view.findViewById<Button>(R.id.buttonKurang)
        val buttonHapus = view.findViewById<ImageButton>(R.id.buttonHapus)
        val textStokHabis = view.findViewById<TextView>(R.id.textStokHabis)
        val layoutJumlah = view.findViewById<View>(R.id.layoutJumlah)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pembeli_item_keranjang, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.nama.text = item.namaMenu

        val hargaInt = item.harga.replace("[^\\d]".toRegex(), "").toIntOrNull() ?: 0
        val formatter = java.text.NumberFormat
            .getCurrencyInstance(java.util.Locale("id", "ID"))
            .apply { maximumFractionDigits = 0 }
        holder.harga.text = formatter.format(hargaInt)

        holder.jumlah.text = item.jumlah.toString()

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_default_menu)
            .into(holder.image)

        // ===== STOK =====
        if (item.stok <= 0) {
            holder.textStokHabis.visibility = View.VISIBLE
            holder.layoutJumlah.visibility = View.GONE
            holder.checkBox.isEnabled = false
            item.isChecked = false
        } else {
            holder.textStokHabis.visibility = View.GONE
            holder.layoutJumlah.visibility = View.VISIBLE
            holder.checkBox.isEnabled = true
        }

        // ===== CHECKBOX =====
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = item.isChecked

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isChecked = isChecked
            onSelectionChanged(getSelectedItems())
        }

        // ===== TAMBAH =====
        holder.buttonTambah.setOnClickListener {
            if (item.jumlah < item.stok) {
                updateJumlahDiFirestore(item.docId, item.jumlah + 1)
            } else {
                Toast.makeText(
                    holder.itemView.context,
                    "Jumlah tidak boleh melebihi stok",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // ===== KURANG =====
        holder.buttonKurang.isEnabled = item.jumlah > 1
        holder.buttonKurang.setOnClickListener {
            if (item.jumlah > 1) {
                updateJumlahDiFirestore(item.docId, item.jumlah - 1)
            }
        }

        // ===== HAPUS =====
        holder.buttonHapus.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            FirebaseFirestore.getInstance()
                .collection("keranjang")
                .document(userId)
                .collection("items")
                .document(item.docId)
                .delete()
                .addOnSuccessListener {
                    onSelectionChanged(getSelectedItems())
                }
        }
    }

    fun getSelectedItems(): List<KeranjangItem> {
        return list.filter { it.isChecked }
    }

    fun clearSelection() {
        list.forEach { it.isChecked = false }
        notifyDataSetChanged()
        onSelectionChanged(emptyList())
    }

    fun updateList(newList: List<KeranjangItem>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
        onSelectionChanged(getSelectedItems())
    }

    private fun updateJumlahDiFirestore(docId: String, jumlahBaru: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (docId.isEmpty()) return

        FirebaseFirestore.getInstance()
            .collection("keranjang")
            .document(userId)
            .collection("items")
            .document(docId)
            .update("jumlah", jumlahBaru)
    }
}
