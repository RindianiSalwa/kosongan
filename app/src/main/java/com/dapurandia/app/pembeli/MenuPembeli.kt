package com.dapurandia.app.pembeli

data class MenuPembeli(
    val idMenu: String = "",
    val nama: String = "",
    val harga: String = "",
    val imageUrl: String = "",
    val deskripsiSingkat: String = "",
    val deskripsiPanjang: String = "",
    val stok: Int = 0
)

