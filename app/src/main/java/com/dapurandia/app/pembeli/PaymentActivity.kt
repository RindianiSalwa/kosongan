package com.dapurandia.app.pembeli

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dapurandia.app.databinding.ActivityPaymentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private var countDownTimer: CountDownTimer? = null
    private var invoiceId: String = ""
    private val apiKey = "API-f533148170e7bf4ca616e3bd717344fd708b49a3f66d6995"
    private val baseUrl = "https://www.bayar.gg/api"

    private val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButtonPayment.setOnClickListener { showBatalConfirmation() }

        val totalAkhir = intent.getLongExtra("TOTAL_AKHIR", 0L)
        val namaPembeli = intent.getStringExtra("NAMA_PEMBELI") ?: "-"
        val noHp = intent.getStringExtra("NO_HP_PEMBELI") ?: "-"
        val alamatUtama = intent.getStringExtra("ALAMAT_UTAMA") ?: ""
        val alamatDetail = intent.getStringExtra("ALAMAT_DETAIL") ?: "-"
        val lat = intent.getDoubleExtra("LAT", 0.0)
        val lng = intent.getDoubleExtra("LNG", 0.0)
        val jarakKm = intent.getDoubleExtra("JARAK_KM", 0.0)
        val ongkir = intent.getIntExtra("ONGKIR", 0)
        val totalHarga = intent.getLongExtra("TOTAL_HARGA", 0L)
        val listItems = intent.getSerializableExtra("ITEMS_CHECKOUT") as? ArrayList<KeranjangItem>
            ?: arrayListOf()
        val catatan = intent.getStringExtra("CATATAN") ?: "-"

        binding.textTotalPayment.text = formatter.format(totalAkhir)

        createPayment(
            amount = totalAkhir,
            description = "Pesanan $namaPembeli",
            customerName = namaPembeli,
            customerPhone = noHp,
            onSuccess = { invoice, qrImageUrl ->
                invoiceId = invoice
                binding.textInvoiceId.text = "Invoice: $invoice"
                loadQrImage(qrImageUrl)
                startCountdownTimer()
            },
            onFailed = {
                Toast.makeText(this, "Gagal membuat pembayaran, coba lagi.", Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        binding.buttonSudahBayar.setOnClickListener {
            if (invoiceId.isEmpty()) return@setOnClickListener
            checkPaymentStatus(
                invoiceId = invoiceId,
                onPaid = {
                    simpanPesananKeFirestore(
                        namaPembeli = namaPembeli,
                        noHp = noHp,
                        alamatUtama = alamatUtama,
                        alamatDetail = alamatDetail,
                        lat = lat,
                        lng = lng,
                        jarakKm = jarakKm,
                        ongkir = ongkir,
                        totalHarga = totalHarga,
                        listItems = listItems,
                        catatan = catatan,
                        invoiceId = invoiceId
                    )
                },
                onPending = {
                    SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE).apply {
                        titleText = "Belum Terbayar"
                        contentText = "Pembayaran belum kami terima. Pastikan kamu sudah scan dan selesaikan pembayaran."
                        confirmText = "OK"
                        show()
                    }
                },
                onExpired = {
                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).apply {
                        titleText = "QR Kadaluarsa"
                        contentText = "QR code sudah tidak berlaku. Silakan buat pesanan baru."
                        confirmText = "OK"
                        setConfirmClickListener { dismissWithAnimation(); finish() }
                        show()
                    }
                }
            )
        }

        binding.buttonBatalPayment.setOnClickListener {
            showBatalConfirmation()
        }
    }

    private fun createPayment(
        amount: Long,
        description: String,
        customerName: String,
        customerPhone: String,
        onSuccess: (invoiceId: String, qrImageUrl: String) -> Unit,
        onFailed: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$baseUrl/create-payment.php")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-API-Key", apiKey)
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val body = JSONObject().apply {
                    put("amount", amount)
                    put("description", description)
                    put("customer_name", customerName)
                    put("customer_phone", customerPhone)
                    put("payment_method", "qris")
                    put("use_qris_converter", true)
                }.toString()

                conn.outputStream.write(body.toByteArray())

                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = BufferedReader(InputStreamReader(stream)).readText()
                val json = JSONObject(response)

                withContext(Dispatchers.Main) {
                    if (json.optBoolean("success", false)) {
                        val data = json.getJSONObject("data")
                        val invoice = data.getString("invoice_id")
                        val qrUrl = data.optString("qris_dynamic_image_url", "")

                        if (qrUrl.isNotEmpty()) {
                            onSuccess(invoice, qrUrl)
                        } else {
                            onFailed()
                        }
                    } else {
                        onFailed()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onFailed() }
            }
        }
    }

    private fun loadQrImage(imageUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(imageUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                conn.connect()
                val bitmap: Bitmap = BitmapFactory.decodeStream(conn.inputStream)
                withContext(Dispatchers.Main) {
                    binding.progressQris.visibility = View.GONE
                    binding.imageQris.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressQris.visibility = View.GONE
                    Toast.makeText(this@PaymentActivity, "Gagal memuat QR code.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkPaymentStatus(
        invoiceId: String,
        onPaid: () -> Unit,
        onPending: () -> Unit,
        onExpired: () -> Unit
    ) {
        val progress = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
            titleText = "Mengecek pembayaran..."
            progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
            setCancelable(false)
            show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$baseUrl/check-payment.php?invoice=$invoiceId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("X-API-Key", apiKey)
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val json = JSONObject(response)
                val status = if (json.has("data")) {
                    json.getJSONObject("data").optString("status", "pending")
                } else {
                    json.optString("status", "pending")
                }

                withContext(Dispatchers.Main) {
                    progress.dismissWithAnimation()
                    when (status) {
                        "paid" -> onPaid()
                        "expired", "cancelled" -> onExpired()
                        else -> onPending()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progress.dismissWithAnimation()
                    Toast.makeText(this@PaymentActivity, "Gagal cek status, coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun simpanPesananKeFirestore(
        namaPembeli: String,
        noHp: String,
        alamatUtama: String,
        alamatDetail: String,
        lat: Double,
        lng: Double,
        jarakKm: Double,
        ongkir: Int,
        totalHarga: Long,
        listItems: ArrayList<KeranjangItem>,
        catatan: String,
        invoiceId: String
    ) {
        val progress = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
            titleText = "Menyimpan pesanan..."
            progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
            setCancelable(false)
            show()
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val itemsMapList = listItems.map {
            mapOf(
                "nama" to it.namaMenu,
                "harga" to it.harga,
                "jumlah" to it.jumlah,
                "gambar" to it.imageUrl
            )
        }

        val pesanan = hashMapOf(
            "idPembeli" to userId,
            "namaPembeli" to namaPembeli,
            "noHp" to noHp,
            "alamat" to hashMapOf(
                "alamatUtama" to alamatUtama,
                "alamatDetail" to alamatDetail,
                "lat" to lat,
                "lng" to lng
            ),
            "catatan" to catatan,
            "status" to "Menunggu",
            "jarakKm" to jarakKm,
            "ongkir" to ongkir,
            "totalHarga" to totalHarga,
            "items" to itemsMapList,
            "metodePembayaran" to "QRIS",
            "invoiceId" to invoiceId,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("pesanan").add(pesanan)
            .addOnSuccessListener {
                for (item in listItems) {
                    db.collection("keranjang")
                        .document(userId)
                        .collection("items")
                        .document(item.docId)
                        .delete()

                    db.collection("menus")
                        .whereEqualTo("nama", item.namaMenu)
                        .get()
                        .addOnSuccessListener { snap ->
                            for (doc in snap) {
                                val ref = doc.reference
                                ref.update("stok", FieldValue.increment(-item.jumlah.toLong()))
                                    .addOnSuccessListener {
                                        ref.get().addOnSuccessListener { updated ->
                                            if ((updated.getLong("stok") ?: 0L) <= 0) {
                                                ref.update("tersedia", false)
                                            }
                                        }
                                    }
                            }
                        }
                }

                countDownTimer?.cancel()
                progress.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                progress.titleText = "Pembayaran Berhasil!"
                progress.contentText = "Pesanan kamu sudah kami terima."
                progress.showCancelButton(false)
                progress.findViewById<android.widget.Button>(
                    cn.pedant.SweetAlert.R.id.confirm_button
                )?.visibility = View.GONE

                binding.buttonSudahBayar.postDelayed({
                    progress.dismissWithAnimation()
                    finishAffinity()
                }, 2000)
            }
            .addOnFailureListener {
                progress.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                progress.titleText = "Gagal Menyimpan"
                progress.contentText = "Coba lagi."
                progress.showCancelButton(false)
                progress.findViewById<android.widget.Button>(
                    cn.pedant.SweetAlert.R.id.confirm_button
                )?.visibility = View.GONE
                binding.buttonSudahBayar.postDelayed({
                    progress.dismissWithAnimation()
                }, 1500)
            }
    }

    private fun startCountdownTimer() {
        val duration = 15 * 60 * 1000L
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.textTimer.text = "Berlaku selama: %02d:%02d".format(minutes, seconds)
            }

            override fun onFinish() {
                binding.textTimer.text = "QR Code sudah kadaluarsa"
                binding.buttonSudahBayar.isEnabled = false
                SweetAlertDialog(this@PaymentActivity, SweetAlertDialog.ERROR_TYPE).apply {
                    titleText = "Waktu Habis"
                    contentText = "QR code sudah kadaluarsa. Silakan buat pesanan baru."
                    confirmText = "OK"
                    setConfirmClickListener { dismissWithAnimation(); finish() }
                    show()
                }
            }
        }.start()
    }

    private fun showBatalConfirmation() {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE).apply {
            titleText = "Batalkan Pembayaran?"
            contentText = "Pesanan tidak akan disimpan jika pembayaran dibatalkan."
            confirmText = "Ya, Batalkan"
            cancelText = "Lanjutkan"
            showCancelButton(true)
            setConfirmClickListener { dismissWithAnimation(); finish() }
            show()
        }
    }

    override fun onBackPressed() {
        showBatalConfirmation()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}