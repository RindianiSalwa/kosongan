package com.dapurandia.app.model

data class Makanan(
    val id: String = "",
    val nama: String = "",
    val harga: Int = 0,
    val deskripsi: String = "",
    val gambarUrl: String = "",
    val stok: Int = 0
)