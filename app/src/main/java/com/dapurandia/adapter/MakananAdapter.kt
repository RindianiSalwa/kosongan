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
import com.example.dapurandia.R

class MakananAdapter(
    private var listMakanan: MutableList<Makanan>,
    private val onItemClick: (Makanan) -> Unit,
    private val onTambahClick: (Makanan) -> Unit
) : RecyclerView.Adapter<MakananAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMakanan: ImageView = view.findViewById(R.id.ivMakanan)
        val tvNama: TextView = view.findViewById(R.id.tvNamaMakanan)
        val tvDeskripsi: TextView = view.findViewById(R.id.tvDeskripsi)
        val tvHarga: TextView = view.findViewById(R.id.tvHarga)
        val btnTambah: Button = view.findViewById(R.id.btnTambah)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_makanan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val makanan = listMakanan[position]

        holder.tvNama.text = makanan.nama
        holder.tvDeskripsi.text = makanan.deskripsi
        holder.tvHarga.text = "Rp ${makanan.harga}"

        Glide.with(holder.itemView.context)
            .load(makanan.gambarUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.ivMakanan)

        holder.itemView.setOnClickListener { onItemClick(makanan) }
        holder.btnTambah.setOnClickListener { onTambahClick(makanan) }
    }

    override fun getItemCount() = listMakanan.size

    // fungsi untuk filter search
    fun filter(keyword: String, listAsli: MutableList<Makanan>) {
        listMakanan = if (keyword.isEmpty()) {
            listAsli
        } else {
            listAsli.filter {
                it.nama.lowercase().contains(keyword.lowercase())
            }.toMutableList()
        }
        notifyDataSetChanged()
    }
}