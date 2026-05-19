package com.dapurandia.app.kurir

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dapurandia.app.R
import com.dapurandia.app.admin.Pesanan
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import cn.pedant.SweetAlert.SweetAlertDialog
import android.os.Handler
import androidx.core.content.ContextCompat
import android.Manifest



class KurirPesananAktifAdapter(
    private val pesananList: List<Pesanan>
) : RecyclerView.Adapter<KurirPesananAktifAdapter.PesananViewHolder>() {

    private val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID"))

    inner class PesananViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaPembeli: TextView = itemView.findViewById(R.id.tvNamaPembeli)
        val tvNoHp: TextView = itemView.findViewById(R.id.tvNoHp)
        val tvAlamat: TextView = itemView.findViewById(R.id.tvAlamat)
        val tvWaktuPesan: TextView = itemView.findViewById(R.id.tvWaktu)
        val tvWaktuUpdate: TextView = itemView.findViewById(R.id.tvWaktuUpdate)
        val tvCatatan: TextView = itemView.findViewById(R.id.tvCatatan)
        val tvTotalHarga: TextView = itemView.findViewById(R.id.tvTotalHarga)
        val tvStatusPesanan: TextView = itemView.findViewById(R.id.tvStatusPesanan)
        val btnAntarSekarang: Button = itemView.findViewById(R.id.btnMasak)

        // 🔥 PENTING: ID HARUS SAMA DENGAN XML
        val rvMenuPesanan: RecyclerView =
            itemView.findViewById(R.id.rvDaftarMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PesananViewHolder {
        val view = LayoutInflater.from(parent.context)
            // 🔥 PAKAI LAYOUT ADMIN
            .inflate(R.layout.admin_item_pesanan, parent, false)
        return PesananViewHolder(view)
    }

    override fun onBindViewHolder(holder: PesananViewHolder, position: Int) {
        val pesanan = pesananList[position]

        holder.tvNamaPembeli.text = pesanan.namaPembeli
        holder.tvNoHp.text = pesanan.noHp
        val alamatUtama = pesanan.alamat?.alamatUtama ?: "Alamat belum tersedia"
        val alamatDetail = pesanan.alamat?.alamatDetail?.takeIf { it.isNotBlank() }

        holder.tvAlamat.text = buildString {
            append(alamatUtama)
            if (alamatDetail != null) {
                append("\nDetail: ")
                append(alamatDetail)
            }
        }

        holder.tvTotalHarga.text =
            "Total: Rp${formatRupiah(pesanan.totalHarga)}"

        // Status
        holder.tvStatusPesanan.text = when (pesanan.status.lowercase()) {
            "siap_diantar" -> "Status: Menunggu Konfirmasi Kurir"
            "diantar" -> "Status: Diantar"
            "selesai" -> "Status: Selesai"
            "dibatalkan" -> "Status: Dibatalkan"
            else -> "Status: ${pesanan.status}"
        }

        // Catatan
        if (!pesanan.catatan.isNullOrEmpty()) {
            holder.tvCatatan.visibility = View.VISIBLE
            holder.tvCatatan.text = "Catatan: ${pesanan.catatan}"
        } else {
            holder.tvCatatan.visibility = View.GONE
        }

        // Waktu Pesan / Update (khusus kurir)
        val waktuTampil = when {
            pesanan.waktuSiapDiantar is Timestamp -> pesanan.waktuSiapDiantar
            pesanan.timestamp is Timestamp -> pesanan.timestamp
            else -> null
        }

        if (waktuTampil != null) {
            holder.tvWaktuPesan.visibility = View.VISIBLE
            holder.tvWaktuPesan.text =
                "Update: ${formatTimestamp(waktuTampil)}"
        } else {
            holder.tvWaktuPesan.visibility = View.GONE
        }

        holder.tvWaktuUpdate.visibility = View.GONE

        // 🔥 LIST MENU (INI YANG BIKIN TAMPILAN SAMA KAYA PEMBELI)
        holder.rvMenuPesanan.apply {
            layoutManager = LinearLayoutManager(
                holder.itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = KurirItemMenuPesananAdapter(pesanan.items)
        }

        // Tombol Aksi Kurir
        when (pesanan.status.lowercase()) {

            "siap_diantar" -> {
                holder.btnAntarSekarang.visibility = View.VISIBLE
                holder.btnAntarSekarang.text = "Antar Sekarang"

                holder.btnAntarSekarang.setOnClickListener {
                    val context = holder.itemView.context

                    val lat = pesanan.alamat?.lat
                    val lng = pesanan.alamat?.lng

                    if (lat == null || lng == null) {
                        Toast.makeText(context, "Koordinat alamat belum tersedia", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, "Izin lokasi diperlukan untuk navigasi", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val fusedLocationClient =
                        com.google.android.gms.location.LocationServices
                            .getFusedLocationProviderClient(context)

                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location ->
                            if (location == null) {
                                Toast.makeText(context, "Lokasi kurir belum tersedia", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            val intent = Intent(context, MapActivity::class.java).apply {
                                putExtra("lat", lat)
                                putExtra("lng", lng)
                                putExtra("originLat", location.latitude)
                                putExtra("originLng", location.longitude)
                                putExtra("idPembeli", pesanan.idPembeli)
                                putExtra("idPesanan", pesanan.id)
                            }

                            context.startActivity(intent)

                            // update status ke "diantar"
                            FirebaseFirestore.getInstance()
                                .collection("pesanan")
                                .document(pesanan.id)
                                .update(
                                    mapOf(
                                        "status" to "diantar",
                                        "waktuDiantar" to Timestamp.now()
                                    )
                                )
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal mendapatkan lokasi kurir", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            else -> {
                holder.btnAntarSekarang.visibility = View.GONE
            }
        }
    }
        private fun formatRupiah(nominal: Long): String {
        val localeID = Locale("in", "ID")
        val formatter = NumberFormat.getNumberInstance(localeID)
        return formatter.format(nominal)
    }

    override fun getItemCount(): Int = pesananList.size

    private fun formatTimestamp(value: Any?): String {
        return when (value) {
            is Timestamp -> sdf.format(value.toDate())
            is Date -> sdf.format(value)
            else -> "-"
        }
    }
}
