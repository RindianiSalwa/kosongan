package com.dapurandia.app.admin

import androidx.annotation.Keep


@Keep
data class ItemPesanan(
    val nama: String = "",
    val jumlah: Int = 0,
    val harga: String="",
    val gambar: String = ""
)
