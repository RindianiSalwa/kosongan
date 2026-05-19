package com.dapurandia.app.admin

data class MenuAdmin(
    val idMenu: String, // field unik baru
    val nama: String,
    val harga: String,
    val imageUrl: String,
    val stok: Int,
    val deskripsiSingkat: String,
    val deskripsiPanjang: String,
    var isSelected: Boolean = false
)
