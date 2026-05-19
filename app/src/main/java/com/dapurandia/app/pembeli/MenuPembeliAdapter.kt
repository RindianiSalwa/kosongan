package com.dapurandia.app.pembeli

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dapurandia.app.R

class MenuPembeliAdapter(
    private var menuList: List<MenuPembeli>,
    private val onItemClick: (MenuPembeli) -> Unit
) : RecyclerView.Adapter<MenuPembeliAdapter.MenuViewHolder>() {

    private var originalList: List<MenuPembeli> = menuList.toList()

    inner class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nama: TextView = view.findViewById(R.id.textNama)
        val harga: TextView = view.findViewById(R.id.textHarga)
        val image: ImageView = view.findViewById(R.id.imgMenu)
        val deskripsiSingkat: TextView = view.findViewById(R.id.textViewDeskripsiSingkat)
        val progressImage: ProgressBar = view.findViewById(R.id.progressImage)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(menuList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pembeli_item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = menuList[position]
        holder.nama.text = item.nama

        val hargaClean = item.harga.replace(".", "").replace(",", "")
        val hargaInt = hargaClean.toIntOrNull() ?: 0
        val formatter =
            java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply {
                maximumFractionDigits = 0
            }
        holder.harga.text = formatter.format(hargaInt)

        holder.deskripsiSingkat.text = item.deskripsiSingkat

        // tampilkan progress bar sebelum load gambar
        holder.progressImage.visibility = View.VISIBLE

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_default_menu)
            .error(R.drawable.ic_default_menu)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // aktifkan cache
            .thumbnail(0.1f) // load versi kecil dulu
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.progressImage.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.progressImage.visibility = View.GONE
                    return false
                }
            })
            .into(holder.image)
    }

    override fun getItemCount(): Int = menuList.size

    fun updateData(newData: List<MenuPembeli>) {
        Log.d("AdapterUpdate", "Memperbarui adapter dengan ${newData.size} data")
        originalList = ArrayList(newData)
        menuList = ArrayList(newData)
        notifyDataSetChanged()
    }

    fun filterList(query: String): Boolean {
        menuList = if (query.isBlank()) {
            originalList
        } else {
            originalList.filter {
                it.nama.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
        return menuList.isEmpty()
    }
}
