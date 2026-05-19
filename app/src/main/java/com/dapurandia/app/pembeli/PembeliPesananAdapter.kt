package com.dapurandia.app.pembeli

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dapurandia.app.R
import com.dapurandia.app.admin.ItemMenuAdapter
import com.dapurandia.app.admin.Pesanan
import com.dapurandia.app.databinding.AdminItemPesananBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PembeliPesananAdapter(
    private val enablePesanLagi: Boolean = false,
    private val onPesanLagiClick: ((Pesanan) -> Unit)? = null,
    private val showBatal: Boolean = true
) : RecyclerView.Adapter<PembeliPesananAdapter.ViewHolder>() {

    private var listPesanan: List<Pesanan> = emptyList()

    fun submitList(data: List<Pesanan>) {
        listPesanan = data
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: AdminItemPesananBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pesanan: Pesanan) {
            binding.tvNamaPembeli.text = pesanan.namaPembeli
            binding.tvNoHp.text = pesanan.noHp
            val alamatUtama = pesanan.alamat?.alamatUtama ?: "Alamat belum tersedia"
            val alamatDetail = pesanan.alamat?.alamatDetail?.takeIf { it.isNotBlank() } ?: "-"

            binding.tvAlamat.text =
                alamatUtama + "\nDetail: " + alamatDetail


            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID"))
            if (pesanan.timestamp is Timestamp) {
                try {
                    binding.tvWaktuUpdate.text = sdf.format((pesanan.timestamp as Timestamp).toDate())
                    binding.tvWaktuUpdate.visibility = android.view.View.VISIBLE
                } catch (e: Exception) {
                    binding.tvWaktuUpdate.visibility = android.view.View.GONE
                }
            } else {
                binding.tvWaktuUpdate.visibility = android.view.View.GONE
            }

            val formattedHarga =
                NumberFormat.getNumberInstance(Locale("in", "ID")).format(pesanan.totalHarga)
            binding.tvTotalHarga.text = "Total: Rp$formattedHarga"

            binding.tvStatusPesanan.text =
                "Status: ${statusTextPembeli(pesanan.status)}"
            when (pesanan.status.lowercase()) {
                "selesai" -> binding.tvStatusPesanan.setTextColor(
                    binding.root.context.getColor(R.color.green)
                )
                "dibatalkan" -> binding.tvStatusPesanan.setTextColor(
                    binding.root.context.getColor(R.color.red_cancel)
                )
                "diantar" -> binding.tvStatusPesanan.setTextColor(
                    binding.root.context.getColor(R.color.green)
                )
                "siap_diantar" -> binding.tvStatusPesanan.setTextColor(
                    binding.root.context.getColor(R.color.orange)
                )
                else -> binding.tvStatusPesanan.setTextColor(
                    binding.root.context.getColor(R.color.orange)
                )
            }

            binding.rvDaftarMenu.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = ItemMenuAdapter(pesanan.items)
            }

            binding.tvCatatan.text =
                "Catatan: ${pesanan.catatan?.takeIf { it.isNotBlank() } ?: "-"}"
            binding.tvCatatan.visibility = android.view.View.VISIBLE

            // Tombol sesuai status
            when (pesanan.status.lowercase()) {
                "menunggu" -> {
                    if (showBatal) {
                        binding.btnMasak.visibility = android.view.View.VISIBLE
                        binding.btnMasak.text = "Batal"
                        binding.btnMasak.setBackgroundColor(binding.root.context.getColor(R.color.red_cancel))
                        binding.btnMasak.setOnClickListener { batalPesanan(pesanan) }
                    } else binding.btnMasak.visibility = android.view.View.GONE
                }
                "diantar" -> {
                    binding.btnMasak.visibility = android.view.View.VISIBLE
                    binding.btnMasak.text = "Lacak Pesanan"
                    binding.btnMasak.setBackgroundColor(
                        binding.root.context.getColor(R.color.orange)
                    )

                    binding.btnMasak.setOnClickListener {
                        val intent = Intent(binding.root.context, TrackingActivity::class.java)
                        intent.putExtra("idPesanan", pesanan.id)
                        intent.putExtra("destLat", pesanan.alamat?.lat ?:0.0)
                        intent.putExtra("destLng", pesanan.alamat?.lng ?:0.0)
                        binding.root.context.startActivity(intent)
                    }
                }
                "selesai", "dibatalkan" -> {
                    if (enablePesanLagi && onPesanLagiClick != null) {
                        binding.btnMasak.visibility = android.view.View.VISIBLE
                        binding.btnMasak.text = "Pesan Lagi"
                        binding.btnMasak.setOnClickListener { onPesanLagiClick.invoke(pesanan) }
                    } else binding.btnMasak.visibility = android.view.View.GONE
                }
                else -> binding.btnMasak.visibility = android.view.View.GONE
            }
        }
        private fun batalPesanan(pesanan: Pesanan) {
            val db = FirebaseFirestore.getInstance()
            val pesananRef = db.collection("pesanan").document(pesanan.id)
            pesananRef.get().addOnSuccessListener { doc ->
                val statusSekarang = doc.getString("status") ?: ""
                if (statusSekarang != "Dibatalkan") {
                    val batch = db.batch()
                    var counter = 0
                    pesanan.items.forEach { item ->
                        db.collection("menus")
                            .whereEqualTo("nama", item.nama)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.documents.forEach { menuDoc ->
                                    val stokSekarang = menuDoc.getLong("stok") ?: 0
                                    val stokBaru = stokSekarang + item.jumlah
                                    batch.update(menuDoc.reference, "stok", stokBaru)
                                }
                                counter++
                                if (counter == pesanan.items.size) {
                                    batch.update(
                                        pesananRef, mapOf(
                                            "status" to "Dibatalkan",
                                            "waktuDibatalkan" to Timestamp.now(),
                                            "timestamp" to Timestamp.now()
                                        )
                                    )
                                    batch.commit()
                                        .addOnSuccessListener {
                                            showDialog("Pesanan dibatalkan", SweetAlertDialog.SUCCESS_TYPE)
                                        }
                                        .addOnFailureListener {
                                            showDialog("Gagal membatalkan pesanan", SweetAlertDialog.ERROR_TYPE)
                                        }
                                }
                            }
                    }
                }
            }
        }

        private fun selesaiPesanan(pesanan: Pesanan) {
            FirebaseFirestore.getInstance()
                .collection("pesanan")
                .document(pesanan.id)
                .update(
                    mapOf(
                        "status" to "selesai",
                        "waktuSelesai" to Timestamp.now(),
                        "timestamp" to Timestamp.now()
                    )
                )
                .addOnSuccessListener {
                    showDialog("Pesanan diselesaikan", SweetAlertDialog.SUCCESS_TYPE)
                }
                .addOnFailureListener {
                    showDialog("Gagal menyelesaikan pesanan", SweetAlertDialog.ERROR_TYPE)
                }
        }

        private fun showDialog(msg: String, type: Int) {
            val dialog = SweetAlertDialog(binding.root.context, type)
                .setTitleText(msg)
                .hideConfirmButton()
            dialog.show()
            android.os.Handler().postDelayed({
                dialog.dismissWithAnimation()
            }, 1500)
        }
    }
    private fun statusTextPembeli(status: String): String {
        return when (status.lowercase()) {
            "menunggu" -> "Menunggu"
            "dimasak" -> "Sedang Dimasak"
            "siap_diantar" -> "Menunggu Konfirmasi Kurir"
            "diantar" -> "Diantar"
            "selesai" -> "Selesai"
            "dibatalkan" -> "Dibatalkan"
            else -> status
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            AdminItemPesananBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = listPesanan.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listPesanan[position])
    }
}
