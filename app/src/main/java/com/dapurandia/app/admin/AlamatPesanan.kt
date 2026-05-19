package com.dapurandia.app.admin

data class AlamatPesanan(
    val alamatUtama: String = "",
    val alamatDetail: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)
