package com.dapurandia.app.pembeli

data class KeranjangItem(
    val namaMenu: String = "",
    val harga: String = "",
    var jumlah: Int = 1,
    val imageUrl: String = "",
    var docId: String = "",
    var stok: Int = 0,
    var isChecked: Boolean = false
) : java.io.Serializable

