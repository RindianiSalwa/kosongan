package com.dapurandia.app.utils

import com.dapurandia.app.model.Makanan

object KeranjangManager {

    // map key = makananId, value = pair(makanan, jumlah)
    private val keranjang = mutableMapOf<String, Pair<Makanan, Int>>()

    fun tambah(makanan: Makanan) {
        val existing = keranjang[makanan.id]
        if (existing != null) {
            keranjang[makanan.id] = Pair(makanan, existing.second + 1)
        } else {
            keranjang[makanan.id] = Pair(makanan, 1)
        }
    }

    fun kurang(makananId: String) {
        val existing = keranjang[makananId] ?: return
        if (existing.second <= 1) {
            keranjang.remove(makananId)
        } else {
            keranjang[makananId] = Pair(existing.first, existing.second - 1)
        }
    }

    fun hapus(makananId: String) {
        keranjang.remove(makananId)
    }

    fun getKeranjang(): Map<String, Pair<Makanan, Int>> = keranjang

    fun totalHarga(): Int {
        return keranjang.values.sumOf { it.first.harga * it.second }
    }

    fun totalItem(): Int {
        return keranjang.values.sumOf { it.second }
    }

    fun kosongkan() {
        keranjang.clear()
    }
}