package com.dapurandia.app.pembeli

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.contract.ActivityResultContracts
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dapurandia.app.databinding.PembeliActivityCheckoutBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: PembeliActivityCheckoutBinding
    private lateinit var menuAdapter: MenuCheckoutAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var listItems: ArrayList<KeranjangItem>
    private var latUser: Double? = null
    private var lngUser: Double? = null
    private var totalHargaGlobal = 0
    private var ongkirGlobal = 0
    private var jarakGlobal = 0.0
    private var alamatUser: String? = null
    private var alamatUtama: String? = null
    private var alamatDetail: String? = null

    private val pilihLokasiLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            latUser = data?.getDoubleExtra("LAT", 0.0)
            lngUser = data?.getDoubleExtra("LNG", 0.0)
            alamatUtama = data?.getStringExtra("ALAMAT_UTAMA")
            alamatDetail = data?.getStringExtra("ALAMAT_DETAIL")
            binding.textAlamat.text = alamatUtama
            binding.textAlamatDetail.text = "Detail: $alamatDetail"
            alamatUser = alamatUtama
            updateOngkirDanTotal()
        }
    }

    companion object {
        const val LAT_DAPUR = -6.496494
        const val LNG_DAPUR = 107.449232
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PembeliActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButtonCheckout.setOnClickListener { finish() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ambilLokasiAwalCheckout()

        val nama = intent.getStringExtra("NAMA_PEMBELI") ?: "-"
        val noHp = intent.getStringExtra("NO_HP_PEMBELI") ?: "-"
        listItems = intent.getSerializableExtra("ITEMS_CHECKOUT") as? ArrayList<KeranjangItem>
            ?: arrayListOf()

        binding.textNama.text = nama
        binding.textNoHp.text = noHp

        binding.btnGantiAlamat.setOnClickListener {
            val bottomSheet = PilihLokasiBottomSheet(
                latUser,
                lngUser,
                alamatUtama
            ) { lat, lng, alamatUtamaResult, alamatDetailResult ->
                latUser = lat
                lngUser = lng
                alamatUtama = alamatUtamaResult
                alamatDetail = alamatDetailResult
                alamatUser = alamatUtamaResult
                binding.textAlamat.text = alamatUtama
                binding.textAlamatDetail.text =
                    if (alamatDetail.isNullOrEmpty()) "Detail: -" else "Detail: $alamatDetail"
                updateOngkirDanTotal()
            }
            bottomSheet.show(supportFragmentManager, "PilihLokasi")
        }

        menuAdapter = MenuCheckoutAdapter(listItems)
        binding.recyclerMenuDipesan.layoutManager = LinearLayoutManager(this)
        binding.recyclerMenuDipesan.adapter = menuAdapter

        totalHargaGlobal = listItems.sumOf {
            it.jumlah * (it.harga.toIntOrNull() ?: 0)
        }

        val formatter = java.text.NumberFormat
            .getCurrencyInstance(java.util.Locale("id", "ID"))
            .apply { maximumFractionDigits = 0 }

        binding.textTotal.text = "Total: ${formatter.format(totalHargaGlobal)}"

        binding.buttonKonfirmasi.setOnClickListener { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            val alamat = alamatUser ?: ""
            if (alamat.isEmpty()) {
                Toast.makeText(this, "Harap pilih alamat pengiriman!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (listItems.isEmpty()) {
                Toast.makeText(this, "Tidak ada item yang dipilih!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val catatanUser = binding.editCatatan?.text.toString().trim()
            val finalCatatan = if (catatanUser.isEmpty()) "-" else catatanUser
            val totalHargaDb = listItems.sumOf { (it.harga.toLongOrNull() ?: 0L) * it.jumlah }
            val totalAkhir = totalHargaDb + ongkirGlobal
            val metodeBayar = if (binding.radioCod.isChecked) "COD" else "QRIS"

            if (metodeBayar == "QRIS") {
                val intent = Intent(this, PaymentActivity::class.java).apply {
                    putExtra("TOTAL_AKHIR", totalAkhir)
                    putExtra("TOTAL_HARGA", totalHargaDb)
                    putExtra("NAMA_PEMBELI", binding.textNama.text.toString().trim())
                    putExtra("NO_HP_PEMBELI", binding.textNoHp.text.toString().trim())
                    putExtra("ALAMAT_UTAMA", alamatUtama)
                    putExtra("ALAMAT_DETAIL", alamatDetail)
                    putExtra("LAT", latUser ?: 0.0)
                    putExtra("LNG", lngUser ?: 0.0)
                    putExtra("JARAK_KM", jarakGlobal)
                    putExtra("ONGKIR", ongkirGlobal)
                    putExtra("CATATAN", finalCatatan)
                    putExtra("ITEMS_CHECKOUT", listItems)
                }
                startActivity(intent)
            } else {
                simpanPesananCOD(
                    namaPembeli = binding.textNama.text.toString().trim(),
                    noHp = binding.textNoHp.text.toString().trim(),
                    finalCatatan = finalCatatan,
                    totalHargaDb = totalHargaDb
                )
            }
        }
    }

    private fun simpanPesananCOD(
        namaPembeli: String,
        noHp: String,
        finalCatatan: String,
        totalHargaDb: Long
    ) {
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
                "lat" to latUser,
                "lng" to lngUser
            ),
            "catatan" to finalCatatan,
            "status" to "Menunggu",
            "jarakKm" to jarakGlobal,
            "ongkir" to ongkirGlobal,
            "totalHarga" to totalHargaDb,
            "items" to itemsMapList,
            "metodePembayaran" to "COD",
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
            titleText = "Memproses pesanan..."
            progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
            setCancelable(false)
            show()
        }

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

                progressDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                progressDialog.titleText = "Berhasil!"
                progressDialog.contentText = "Pesanan kamu sudah dikirim."
                progressDialog.showCancelButton(false)
                progressDialog.findViewById<android.widget.Button>(
                    cn.pedant.SweetAlert.R.id.confirm_button
                )?.visibility = android.view.View.GONE

                binding.buttonKonfirmasi.postDelayed({
                    progressDialog.dismissWithAnimation()
                    finish()
                }, 1500)
            }
            .addOnFailureListener {
                progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                progressDialog.titleText = "Oops..."
                progressDialog.contentText = "Gagal mengirim pesanan, coba lagi."
                progressDialog.showCancelButton(false)
                progressDialog.findViewById<android.widget.Button>(
                    cn.pedant.SweetAlert.R.id.confirm_button
                )?.visibility = android.view.View.GONE

                binding.buttonKonfirmasi.postDelayed({
                    progressDialog.dismissWithAnimation()
                }, 1500)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            ambilLokasiAwalCheckout()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            currentFocus!!.clearFocus()
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hitungJarakKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) *
                Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    private fun hitungOngkir(jarakKm: Double): Int {
        return when {
            jarakKm <= 1.0 -> 5000
            jarakKm <= 3.0 -> 8000
            jarakKm <= 5.0 -> 12000
            else -> 15000
        }
    }

    private fun updateOngkirDanTotal() {
        if (latUser == null || lngUser == null) return

        jarakGlobal = hitungJarakKm(LAT_DAPUR, LNG_DAPUR, latUser!!, lngUser!!)
        ongkirGlobal = hitungOngkir(jarakGlobal)

        val formatter = java.text.NumberFormat
            .getCurrencyInstance(java.util.Locale("id", "ID"))
            .apply { maximumFractionDigits = 0 }

        binding.textOngkir.text = "Ongkir: ${formatter.format(ongkirGlobal)}"
        binding.textJarak.text = "Jarak: ${String.format("%.1f km", jarakGlobal)}"

        val totalAkhir = totalHargaGlobal + ongkirGlobal
        binding.textTotal.text = "Total: ${formatter.format(totalAkhir)}"
    }

    private fun ambilLokasiAwalCheckout() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                latUser = location.latitude
                lngUser = location.longitude

                try {
                    val geocoder = Geocoder(this, java.util.Locale.getDefault())
                    val addresses = geocoder.getFromLocation(latUser!!, lngUser!!, 1)
                    if (!addresses.isNullOrEmpty()) {
                        alamatUser = addresses[0].getAddressLine(0)
                        alamatUtama = alamatUser
                        alamatDetail = "-"
                        binding.textAlamat.text = alamatUtama
                        binding.textAlamatDetail.text = "Detail: -"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                updateOngkirDanTotal()
            }
        }
    }
}
