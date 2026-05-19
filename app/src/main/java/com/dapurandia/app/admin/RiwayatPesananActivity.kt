package com.dapurandia.app.admin

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dapurandia.app.R
import com.dapurandia.app.databinding.AdminActivityRiwayatPesananBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RiwayatPesananActivity : AppCompatActivity() {

    private lateinit var binding: AdminActivityRiwayatPesananBinding
    private lateinit var adapter: PesananAdapter
    private val db = FirebaseFirestore.getInstance()

    private var allPesanan: List<Pesanan> = emptyList()
    private var activeFilter = "semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminActivityRiwayatPesananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarRiwayatPesanan)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Riwayat Pesanan"

        binding.toolbarRiwayatPesanan.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        adapter = PesananAdapter(listPesanan = emptyList(), hideButton = true)
        binding.rvRiwayatPesanan.layoutManager = LinearLayoutManager(this)
        binding.rvRiwayatPesanan.adapter = adapter

        setupChipFilter()
        runEntranceAnimations()
        loadRiwayatPesanan()
    }

    private fun setupChipFilter() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            activeFilter = when {
                checkedIds.contains(R.id.chipMenunggu) -> "menunggu"
                checkedIds.contains(R.id.chipDiproses) -> "diproses"
                checkedIds.contains(R.id.chipSelesai) -> "selesai"
                checkedIds.contains(R.id.chipDibatalkan) -> "dibatalkan"
                else -> "semua"
            }
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = if (activeFilter == "semua") {
            allPesanan
        } else {
            allPesanan.filter { it.status == activeFilter }
        }

        binding.rvRiwayatPesanan.animate()
            .alpha(0f)
            .setDuration(120)
            .withEndAction {
                adapter.updateData(filtered)
                updateEmptyState(filtered.isEmpty())
                updateJumlahLabel(filtered.size)
                binding.rvRiwayatPesanan.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutKosong.visibility = View.VISIBLE
            binding.rvRiwayatPesanan.visibility = View.GONE
            binding.layoutKosong.alpha = 0f
            binding.layoutKosong.translationY = 24f
            binding.layoutKosong.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            binding.layoutKosong.visibility = View.GONE
            binding.rvRiwayatPesanan.visibility = View.VISIBLE
        }
    }

    private fun updateJumlahLabel(count: Int) {
        val label = if (activeFilter == "semua") "$count pesanan" else "$count pesanan · $activeFilter"
        binding.tvJumlahHasil.text = label.replaceFirstChar { it.uppercase() }
    }

    private fun runEntranceAnimations() {
        val views = listOf(
            binding.lottieRiwayat,
            binding.tvHeaderTitle,
            binding.tvHeaderSub,
            binding.cardFilter,
            binding.cardRvWrapper
        )
        val delays = listOf(0L, 100L, 160L, 280L, 400L)

        views.forEach {
            it.alpha = 0f
            it.translationY = 48f
        }

        views.zip(delays).forEach { (view, delay) ->
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(450)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_riwayat_pesanan, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cetak -> {
                showDateRangePicker()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadRiwayatPesanan() {
        db.collection("pesanan")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                allPesanan = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Pesanan::class.java)?.copy(id = doc.id)
                }

                applyFilter()
            }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Pilih Periode (7 hari)")
            .build()

        picker.addOnPositiveButtonClickListener { range ->
            val start = Date(range.first!!)
            val end = Date(range.second!!)
            val diff = (end.time - start.time) / (1000 * 60 * 60 * 24)
            if (diff != 6L) {
                Toast.makeText(this, "Pilih rentang tepat 7 hari!", Toast.LENGTH_SHORT).show()
                return@addOnPositiveButtonClickListener
            }
            cetakLaporanDenganRange(start, end)
        }

        picker.show(supportFragmentManager, "date_range_picker")
    }

    private fun cetakLaporanDenganRange(start: Date, end: Date) {
        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Membuat laporan..."
        progressDialog.progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
        progressDialog.setCancelable(false)
        progressDialog.show()

        db.collection("pesanan")
            .whereEqualTo("status", "selesai")
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(start))
            .whereLessThanOrEqualTo("timestamp", Timestamp(end))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { d ->
                    d.toObject(Pesanan::class.java)?.copy(id = d.id)
                }

                if (list.isEmpty()) {
                    progressDialog.dismissWithAnimation()
                    Toast.makeText(this, "Tidak ada pesanan selesai di periode ini.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val file = tulisPdfLaporan(list, start, end)
                if (file != null) {
                    progressDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                    progressDialog.titleText = "Laporan berhasil dibuat!"
                    progressDialog.contentText = "File tersimpan di folder aplikasi."
                    progressDialog.show()
                    progressDialog.findViewById<View>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE
                    progressDialog.findViewById<View>(cn.pedant.SweetAlert.R.id.cancel_button)?.visibility = View.GONE

                    Handler(Looper.getMainLooper()).postDelayed({
                        progressDialog.dismissWithAnimation()
                        bukaPdf(file)
                    }, 2000)
                } else {
                    progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                    progressDialog.titleText = "Gagal membuat laporan"
                    progressDialog.setConfirmText("OK")
                }
            }
            .addOnFailureListener {
                progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                progressDialog.titleText = "Gagal ambil data laporan"
                progressDialog.setConfirmText("OK")
            }
    }

    private fun tulisPdfLaporan(orders: List<Pesanan>, start: Date, end: Date): File? {
        return try {
            val localeID = Locale("in", "ID")
            val dateFmtHeader = SimpleDateFormat("dd MMM yyyy", localeID)
            val dateFmtRow = SimpleDateFormat("dd MMM yyyy, HH:mm", localeID)
            val currencyFmt = NumberFormat.getNumberInstance(localeID)

            val pdf = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 32
            val lineHeight = 18
            var pageNumber = 1
            var y = margin + 20

            fun startPage(): PdfDocument.Page {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdf.startPage(pageInfo)
                val c = page.canvas
                val titlePaint = Paint().apply { isAntiAlias = true; textSize = 16f; isFakeBoldText = true }
                val normalPaint = Paint().apply { isAntiAlias = true; textSize = 12f }
                c.drawText("Laporan Pesanan Selesai", margin.toFloat(), (margin + 5).toFloat(), titlePaint)
                c.drawText("Dapur Andia", margin.toFloat(), (margin + 5 + lineHeight).toFloat(), titlePaint)
                c.drawText(
                    "Periode: ${dateFmtHeader.format(start)} - ${dateFmtHeader.format(end)}",
                    margin.toFloat(), (margin + 5 + lineHeight * 2).toFloat(), normalPaint
                )
                val headerY = margin + 5 + lineHeight * 4
                val bold = Paint(normalPaint).apply { isFakeBoldText = true }
                c.drawRect(
                    margin.toFloat(), (headerY - lineHeight).toFloat(),
                    (pageWidth - margin).toFloat(), (headerY + 6).toFloat(),
                    Paint().apply { style = Paint.Style.FILL; color = 0xFFE0E0E0.toInt() }
                )
                c.drawText("No", margin.toFloat(), headerY.toFloat(), bold)
                c.drawText("Tanggal", (margin + 40).toFloat(), headerY.toFloat(), bold)
                c.drawText("Pembeli", (margin + 180).toFloat(), headerY.toFloat(), bold)
                c.drawText("Total (Rp)", (pageWidth - margin - 100).toFloat(), headerY.toFloat(), bold)
                y = headerY + lineHeight
                return page
            }

            var page = startPage()
            val normalPaint = Paint().apply { isAntiAlias = true; textSize = 12f }
            var no = 1
            var totalSemua = 0L

            fun ensureSpace() {
                if (y > pageHeight - margin - 60) {
                    pdf.finishPage(page)
                    pageNumber++
                    page = startPage()
                }
            }

            orders.forEach { p ->
                ensureSpace()
                val tanggal = when (val t = p.waktuSelesai ?: p.timestamp) {
                    is Timestamp -> dateFmtRow.format(t.toDate())
                    is Date -> dateFmtRow.format(t)
                    else -> "-"
                }
                page.canvas.drawText(no.toString(), margin.toFloat(), y.toFloat(), normalPaint)
                page.canvas.drawText(tanggal, (margin + 40).toFloat(), y.toFloat(), normalPaint)
                page.canvas.drawText(p.namaPembeli, (margin + 180).toFloat(), y.toFloat(), normalPaint)
                page.canvas.drawText(currencyFmt.format(p.totalHarga), (pageWidth - margin - 100).toFloat(), y.toFloat(), normalPaint)
                y += lineHeight

                p.items.forEach { item ->
                    ensureSpace()
                    val hargaItem = try { item.harga.toLong() } catch (e: Exception) { 0L }
                    page.canvas.drawText(
                        "- ${item.nama} x${item.jumlah} (${currencyFmt.format(hargaItem)})",
                        (margin + 60).toFloat(), y.toFloat(), normalPaint
                    )
                    y += lineHeight
                }

                totalSemua += p.totalHarga
                no++
            }

            val bold = Paint(normalPaint).apply { isFakeBoldText = true }
            page.canvas.drawLine(
                (pageWidth - margin - 140).toFloat(), (y + 4).toFloat(),
                (pageWidth - margin).toFloat(), (y + 4).toFloat(), Paint()
            )
            page.canvas.drawText("TOTAL", (pageWidth - margin - 140).toFloat(), (y + lineHeight).toFloat(), bold)
            page.canvas.drawText(currencyFmt.format(totalSemua), (pageWidth - margin - 100).toFloat(), (y + lineHeight).toFloat(), bold)

            pdf.finishPage(page)

            val dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (dir?.exists() == false) dir.mkdirs()
            val file = File(dir, "Laporan_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf")
            FileOutputStream(file).use { fos -> pdf.writeTo(fos) }
            pdf.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun bukaPdf(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Buka laporan PDF"))
        } catch (_: Exception) {
            Toast.makeText(this, "Tidak ada aplikasi pembaca PDF.", Toast.LENGTH_SHORT).show()
        }
    }
}