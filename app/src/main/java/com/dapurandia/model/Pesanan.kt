package com.example.dapurandia.model

data class Pesanan(
    val id: String = "",
    val pembeliId: String = "",
    val pembeliNama: String = "",
    val alamat: String = "",
    val catatan: String = "",
    val items: Map<String, ItemPesanan> = mapOf(),
    val totalHarga: Int = 0,
    val status: String = "MENUNGGU_APPROVAL",
    val kurirId: String = "",
    val timestamp: Long = 0
)

data class ItemPesanan(
    val makananId: String = "",
    val nama: String = "",
    val harga: Int = 0,
    val jumlah: Int = 0
)