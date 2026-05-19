package com.dapurandia.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dapurandia.app.model.Makanan
import com.dapurandia.app.utils.KeranjangManager
import com.example.dapurandia.R

class KeranjangAdapter(
    private var items: MutableList<Pair<Makanan, Int>>,
    private val onUpdate: () -> Unit
) : RecyclerView.Adapter<KeranjangAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMakanan: ImageView = view.findViewById(R.id.ivMakanan)
        val tvNama: TextView = view.findViewById(R.id.tvNama)
        val tvHarga: TextView = view.findViewById(R.id.tvHarga)
        val tvJumlah: TextView = view.findViewById(R.id.tvJumlah)
        val btnTambah: Button = view.findViewById(R.id.btnTambah)
        val btnKurang: Button = view.findViewById(R.id.btnKurang)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_keranjang, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (makanan, jumlah) = items[position]

        holder.tvNama.text = makanan.nama
        holder.tvHarga.text = "Rp ${makanan.harga * jumlah}"
        holder.tvJumlah.text = jumlah.toString()

        Glide.with(holder.itemView.context)
            .load(makanan.gambarUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.ivMakanan)

        holder.btnTambah.setOnClickListener {
            KeranjangManager.tambah(makanan)
            refresh()
            onUpdate()
        }

        holder.btnKurang.setOnClickListener {
            KeranjangManager.kurang(makanan.id)
            refresh()
            onUpdate()
        }
    }

    override fun getItemCount() = items.size

    fun refresh() {
        items = KeranjangManager.getKeranjang().values.toMutableList()
        notifyDataSetChanged()
    }
}