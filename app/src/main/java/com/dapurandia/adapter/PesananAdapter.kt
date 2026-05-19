package com.dapurandia.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.example.dapurandia.R
import com.example.dapurandia.model.Pesanan

class PesananAdapter(
    private var listPesanan: MutableList<Pesanan>,
    private val onDetailClick: (Pesanan) -> Unit,
    private val onApproveClick: (Pesanan) -> Unit
) : RecyclerView.Adapter<PesananAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIdPesanan: TextView = view.findViewById(R.id.tvIdPesanan)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvNamaPembeli: TextView = view.findViewById(R.id.tvNamaPembeli)
        val tvAlamat: TextView = view.findViewById(R.id.tvAlamat)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val btnDetail: Button = view.findViewById(R.id.btnDetail)
        val btnApprove: Button = view.findViewById(R.id.btnApprove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pesanan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pesanan = listPesanan[position]

        holder.tvIdPesanan.text = "#${pesanan.id.takeLast(6).uppercase()}"
        holder.tvStatus.text = pesanan.status
        holder.tvNamaPembeli.text = pesanan.pembeliNama
        holder.tvAlamat.text = pesanan.alamat
        holder.tvTotal.text = "Rp ${pesanan.totalHarga}"

        // sembunyikan tombol approve kalau sudah bukan MENUNGGU_APPROVAL
        if (pesanan.status == "MENUNGGU_APPROVAL") {
            holder.btnApprove.visibility = View.VISIBLE
        } else {
            holder.btnApprove.visibility = View.GONE
        }

        holder.btnDetail.setOnClickListener { onDetailClick(pesanan) }
        holder.btnApprove.setOnClickListener { onApproveClick(pesanan) }
    }

    override fun getItemCount() = listPesanan.size

    fun filter(status: String) {
        // akan dipanggil dari activity
    }

    fun updateData(data: MutableList<Pesanan>) {
        listPesanan = data
        notifyDataSetChanged()
    }
}