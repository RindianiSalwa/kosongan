package com.dapurandia.app.pembeli

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dapurandia.app.R

class MenuCheckoutAdapter(private val menuList: List<KeranjangItem>) :
    RecyclerView.Adapter<MenuCheckoutAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageMenu: ImageView = view.findViewById(R.id.imageMenu)
        val textNamaMenu: TextView = view.findViewById(R.id.textNamaMenu)
        val textJumlah: TextView = view.findViewById(R.id.textJumlah)
        val textHargaSatuan: TextView = view.findViewById(R.id.textHargaSatuan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pembeli_item_menu_checkout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = menuList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = menuList[position]
        holder.textNamaMenu.text = item.namaMenu

        val hargaInt = item.harga.replace("[^\\d]".toRegex(), "").toIntOrNull() ?: 0
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        holder.textHargaSatuan.text = formatter.format(hargaInt)

        holder.textJumlah.text = "x${item.jumlah}"

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_default_menu)
            .into(holder.imageMenu)
    }
}
