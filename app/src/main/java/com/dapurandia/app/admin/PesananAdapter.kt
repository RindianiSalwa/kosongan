package com.dapurandia.app.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dapurandia.app.R
import com.dapurandia.app.databinding.AdminItemPesananBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import cn.pedant.SweetAlert.SweetAlertDialog
import android.os.Handler
import android.os.Looper

class PesananAdapter(
    private var listPesanan: List<Pesanan>,
    private val onStatusSelesaiClick: (Pesanan) -> Unit = {},
    private val showAntarButton: Boolean = false,
    private val isAntarFragment: Boolean = false,
    private val hideButton: Boolean = false,
    private val onStatusUpdated: () -> Unit = {}
) : RecyclerView.Adapter<PesananAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: AdminItemPesananBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pesanan: Pesanan) {
            binding.tvNamaPembeli.text = pesanan.namaPembeli
            binding.tvNoHp.text = pesanan.noHp
            val alamatUtama = pesanan.alamat?.alamatUtama ?: "Alamat belum tersedia"
            val alamatDetail = pesanan.alamat?.alamatDetail?.takeIf { it.isNotBlank() } ?: "-"

            binding.tvAlamat.text =
                alamatUtama + "\nDetail: " + alamatDetail


            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID"))
            if (pesanan.timestamp is Timestamp) {
                val ts = pesanan.timestamp as Timestamp
                binding.tvWaktuUpdate.text = "Update: ${sdf.format(ts.toDate())}"
                binding.tvWaktuUpdate.visibility = View.VISIBLE
            } else {
                binding.tvWaktuUpdate.visibility = View.GONE
            }

            val formattedHarga = NumberFormat.getNumberInstance(Locale("in", "ID")).format(pesanan.totalHarga)
            binding.tvTotalHarga.text = "Total: Rp$formattedHarga"

            binding.tvCatatan.text = "Catatan: ${pesanan.catatan?.takeIf { it.isNotBlank() } ?: "-"}"
            binding.tvCatatan.visibility = View.VISIBLE

            // Status ditampilkan sesuai final alur:
            // siap_diantar -> "Menunggu konfirmasi kurir"
            // diantar -> "Diantar"
            binding.tvStatusPesanan.text = when (pesanan.status.lowercase()) {
                "selesai" -> "Status: Selesai"
                "dibatalkan" -> "Status: Dibatalkan"
                "siap_diantar" -> "Status: Menunggu konfirmasi kurir"
                "diantar" -> "Status: Diantar"
                else -> "Status: ${pesanan.status.capitalize()}"
            }

            when (pesanan.status.lowercase()) {
                "selesai" -> binding.tvStatusPesanan.setTextColor(binding.root.context.getColor(R.color.green))
                "dibatalkan" -> binding.tvStatusPesanan.setTextColor(binding.root.context.getColor(R.color.red_cancel))
                "diantar" -> binding.tvStatusPesanan.setTextColor(binding.root.context.getColor(R.color.green))
                else -> binding.tvStatusPesanan.setTextColor(binding.root.context.getColor(R.color.orange))
            }

            binding.rvDaftarMenu.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = ItemMenuAdapter(pesanan.items)
            }

            val db = FirebaseFirestore.getInstance()

            if (hideButton) {
                binding.btnMasak.visibility = View.GONE
            } else {
                binding.btnMasak.visibility = View.VISIBLE

                fun showProgressDialog(title: String): SweetAlertDialog {
                    val progressDialog = SweetAlertDialog(binding.root.context, SweetAlertDialog.PROGRESS_TYPE)
                    progressDialog.progressHelper.barColor = binding.root.context.getColor(R.color.maroon_700)
                    progressDialog.titleText = title
                    progressDialog.setCancelable(false)
                    progressDialog.show()
                    return progressDialog
                }

                fun showAutoDismissDialog(type: Int, title: String) {
                    val dialog = SweetAlertDialog(binding.root.context, type)
                        .setTitleText(title)
                    dialog.setCancelable(true)
                    dialog.show()
                    dialog.findViewById<View>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE
                    dialog.findViewById<View>(cn.pedant.SweetAlert.R.id.cancel_button)?.visibility = View.GONE
                    Handler(Looper.getMainLooper()).postDelayed({
                        dialog.dismissWithAnimation()
                    }, 1500)
                }

                when {
                    pesanan.status.equals("Menunggu", ignoreCase = true) -> {
                        // Tombol Masak
                        binding.btnMasak.text = "Masak"
                        binding.btnMasak.setBackgroundColor(binding.root.context.getColor(R.color.maroon_700))
                        binding.btnMasak.setOnClickListener {
                            val progress = showProgressDialog("Memproses...")
                            val dataUpdate = mapOf(
                                "status" to "dimasak",
                                "waktuDimasak" to Timestamp.now(),
                                "timestamp" to Timestamp.now()
                            )
                            db.collection("pesanan").document(pesanan.id)
                                .update(dataUpdate)
                                .addOnSuccessListener {
                                    progress.dismissWithAnimation()
                                    showAutoDismissDialog(SweetAlertDialog.SUCCESS_TYPE, "Pesanan dimasak")
                                    onStatusUpdated()
                                }
                                .addOnFailureListener {
                                    progress.dismissWithAnimation()
                                    showAutoDismissDialog(SweetAlertDialog.ERROR_TYPE, "Gagal update status")
                                }
                        }
                    }

                    showAntarButton -> {
                        // Tombol Siap Diantar
                        binding.btnMasak.text = "Siap Diantar"
                        binding.btnMasak.setBackgroundTintList(
                            binding.root.context.getColorStateList(R.color.maroon_700)
                        )
                        binding.btnMasak.setOnClickListener {
                            val progress = showProgressDialog("Memproses...")
                            val dataUpdate = mapOf(
                                "status" to "siap_diantar",
                                "waktuSiapDiantar" to Timestamp.now(),
                                "timestamp" to Timestamp.now()
                            )
                            db.collection("pesanan").document(pesanan.id)
                                .update(dataUpdate)
                                .addOnSuccessListener {
                                    progress.dismissWithAnimation()
                                    showAutoDismissDialog(SweetAlertDialog.SUCCESS_TYPE, "Pesanan siap diantar")
                                    onStatusUpdated()
                                }
                                .addOnFailureListener {
                                    progress.dismissWithAnimation()
                                    showAutoDismissDialog(SweetAlertDialog.ERROR_TYPE, "Gagal update status")
                                }
                        }
                    }

                    isAntarFragment -> {
                        // Fragment Diantar admin (menampilkan tombol Pesanan Selesai)
                        binding.btnMasak.text = "Pesanan Selesai"
                        binding.btnMasak.setOnClickListener {
                            val progress = showProgressDialog("Memproses...")
                            val dataUpdate = mapOf(
                                "status" to "selesai",
                                "waktuSelesai" to Timestamp.now(),
                                "timestamp" to Timestamp.now()
                            )
                            db.collection("pesanan")
                                .document(pesanan.id)
                                .update(dataUpdate)
                                .addOnSuccessListener {
                                    progress.dismissWithAnimation()
                                    showAutoDismissDialog(SweetAlertDialog.SUCCESS_TYPE, "Pesanan selesai")
                                    onStatusSelesaiClick(pesanan)
                                    onStatusUpdated()
                                }
                                .addOnFailureListener {
                                    progress.dismissWithAnimation()
                                    showAutoDismissDialog(SweetAlertDialog.ERROR_TYPE, "Gagal update status")
                                }
                        }
                    }

                    else -> {
                        binding.btnMasak.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminItemPesananBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = listPesanan.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listPesanan[position])
    }

    fun updateData(newList: List<Pesanan>) {
        listPesanan = newList
        notifyDataSetChanged()
    }
}
