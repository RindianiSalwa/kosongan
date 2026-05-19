package com.dapurandia.app.admin

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dapurandia.app.R
import com.dapurandia.app.databinding.AdminItemMenuPesananBinding

class ItemMenuAdapter(
    private val listItem: List<ItemPesanan>
) : RecyclerView.Adapter<ItemMenuAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: AdminItemMenuPesananBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ItemPesanan) {
            Glide.with(binding.imgMenu.context)
                .load(item.gambar)
                .placeholder(R.drawable.ic_default_menu)
                .into(binding.imgMenu)
            Log.d("ItemMenuAdapter", "gambar url: ${item.gambar}")
            binding.tvNamaMenu.text = item.nama
            binding.tvJumlah.text = "x${item.jumlah}"

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminItemMenuPesananBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = listItem.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listItem[position])
    }
}
