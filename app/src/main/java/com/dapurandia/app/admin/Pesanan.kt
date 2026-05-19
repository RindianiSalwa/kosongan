package com.dapurandia.app.admin

import com.google.firebase.Timestamp

data class Pesanan(
    val id: String = "",
    val idPembeli: String = "",
    val namaPembeli: String = "",
    val noHp: String = "",

    val alamat: AlamatPesanan? = null,

    val totalHarga: Long = 0,
    val status: String = "",
    val kurirLat: Double? = null,
    val kurirLng: Double? = null,
    val bearing: Double? = null,
    val catatan: String? = null,

    val items: List<ItemPesanan> = emptyList(),

    val jarakKm: Double = 0.0,
    val ongkir: Int = 0,

    val waktuPesan: Any? = null,
    var waktuDiantar: Any? = null,
    var waktuSiapDiantar: Any? = null,
    var waktuSelesai: Any? = null,
    var waktuDimasak: Any? = null,
    var waktuDibatalkan: Any? = null,

    val timestamp: Any? = null
)

