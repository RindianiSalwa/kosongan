package com.dapurandia.app.admin

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dapurandia.app.R
import java.text.NumberFormat
import java.util.Locale

class MenuAdminAdapter(private var menuList: List<MenuAdmin>) :
    RecyclerView.Adapter<MenuAdminAdapter.MenuViewHolder>() {

    private var originalList: List<MenuAdmin> = menuList.toList()
    private var isSelectionMode = false

    class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nama: TextView = view.findViewById(R.id.textNama)
        val harga: TextView = view.findViewById(R.id.textHarga)
        val image: ImageView = view.findViewById(R.id.imageMenu)
        val deskripsi: TextView = view.findViewById(R.id.textViewDeskripsiSingkat)
        val checkBox: CheckBox = view.findViewById(R.id.checkBoxSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = menuList[position]
        holder.nama.text = item.nama

        val hargaClean = item.harga.replace(".", "").replace(",", "")
        val hargaInt = hargaClean.toIntOrNull() ?: 0
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        val formattedHarga = formatter.format(hargaInt)
        holder.harga.text = formattedHarga

        holder.deskripsi.text = item.deskripsiSingkat

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_default_menu)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.ic_default_menu)
        }

        holder.checkBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = item.isSelected

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                item.isSelected = !item.isSelected
                holder.checkBox.isChecked = item.isSelected
            } else {
                val context = holder.itemView.context
                val intent = Intent(context, DetailMenuAdminActivity::class.java).apply {
                    putExtra("idMenu", item.idMenu)
                    putExtra("namaMenu", item.nama)
                    putExtra("hargaMenu", item.harga)
                    putExtra("stokMenu", item.stok)
                    putExtra("deskripsiPanjang", item.deskripsiPanjang)
                    putExtra("imageUrl", item.imageUrl)
                    putExtra("deskripsiSingkat", item.deskripsiSingkat)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = menuList.size

    fun filterList(query: String): Boolean {
        menuList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter {
                it.nama.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
        return menuList.isEmpty()
    }

    fun updateData(newList: List<MenuAdmin>) {
        menuList = newList
        originalList = newList.toList()
        notifyDataSetChanged()
    }

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) {
            menuList.forEach { it.isSelected = false }
        }
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<MenuAdmin> {
        return menuList.filter { it.isSelected }
    }
}
