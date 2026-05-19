package com.dapurandia.app.kurir

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dapurandia.app.R
import com.dapurandia.app.admin.ItemPesanan

class KurirItemMenuPesananAdapter(
    private val itemMenuList: List<ItemPesanan>
) : RecyclerView.Adapter<KurirItemMenuPesananAdapter.ItemMenuViewHolder>() {

    inner class ItemMenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgMenu: ImageView = itemView.findViewById(R.id.imageMenu)
        val tvNamaMenu: TextView = itemView.findViewById(R.id.textNamaMenu)
        val tvHarga: TextView = itemView.findViewById(R.id.textHargaSatuan)
        val tvJumlah: TextView = itemView.findViewById(R.id.textJumlah)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemMenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            // 🔥 PAKAI XML YANG SAMA
            .inflate(R.layout.pembeli_item_menu_checkout, parent, false)
        return ItemMenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemMenuViewHolder, position: Int) {
        val item = itemMenuList[position]

        holder.tvNamaMenu.text = item.nama
        holder.tvHarga.text = "Harga: Rp${item.harga}"
        holder.tvJumlah.text = "x${item.jumlah}"

        Glide.with(holder.itemView.context)
            .load(item.gambar)
            .placeholder(R.drawable.ic_default_menu)
            .into(holder.imgMenu)
    }

    override fun getItemCount(): Int = itemMenuList.size
}
