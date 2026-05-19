package com.dapurandia.app.admin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.dapurandia.app.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.util.Locale
import cn.pedant.SweetAlert.SweetAlertDialog

class DetailMenuAdminActivity : AppCompatActivity() {

    private lateinit var textViewNamaMenu: TextView
    private lateinit var textViewStok: TextView
    private lateinit var textViewHarga: TextView
    private lateinit var textViewDeskripsi: TextView
    private var idMenu: String? = null
    private var namaMenu: String? = null
    private var imageUrl: String? = null
    private var hargaMenu: String? = null
    private var deskripsiMenu: String? = null
    private var deskripsiSingkat: String? = null

    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_detail_menu)

        val imageViewMenu = findViewById<ImageView>(R.id.imageViewMenu)
        textViewNamaMenu = findViewById(R.id.textViewNamaMenu)
        textViewStok = findViewById(R.id.textViewStok)
        textViewHarga = findViewById(R.id.textViewHarga)
        textViewDeskripsi = findViewById(R.id.textViewDeskripsi)
        val buttonUpdateStok = findViewById<Button>(R.id.buttonUpdateStok)
        val backIcon = findViewById<ImageView>(R.id.backIcon)
        val buttonEditMenu = findViewById<Button>(R.id.buttonEditMenu)

        backIcon.setOnClickListener { finish() }

        idMenu = intent.getStringExtra("idMenu")
        namaMenu = intent.getStringExtra("namaMenu")
        hargaMenu = intent.getStringExtra("hargaMenu")
        val stokMenu = intent.getIntExtra("stokMenu", 0)
        imageUrl = intent.getStringExtra("imageUrl")
        deskripsiMenu = intent.getStringExtra("deskripsiPanjang")
        deskripsiSingkat = intent.getStringExtra("deskripsiSingkat")


        textViewNamaMenu.text = namaMenu

        val hargaInt = hargaMenu?.replace("[^\\d]".toRegex(), "")?.toIntOrNull() ?: 0
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        val formattedHarga = formatter.format(hargaInt)
        textViewHarga.text = "Harga: $formattedHarga"
        textViewStok.text = "Stok: $stokMenu"
        textViewDeskripsi.text = deskripsiMenu

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_default_menu)
                .into(imageViewMenu)
        } else {
            imageViewMenu.setImageResource(R.drawable.ic_default_menu)
        }

        buttonEditMenu.setOnClickListener {
            val intent = Intent(this, EditMenuAdminActivity::class.java).apply {
                putExtra("idMenu", idMenu)
                putExtra("namaMenu", textViewNamaMenu.text.toString())
                putExtra("hargaMenu", textViewHarga.text.toString().replace("[^\\d]".toRegex(), ""))
                putExtra(
                    "stokMenu",
                    textViewStok.text.toString().substringAfter(": ").toIntOrNull() ?: 0
                )
                putExtra("imageUrl", imageUrl)
                putExtra("deskripsiPanjang", textViewDeskripsi.text.toString())
                putExtra("deskripsiSingkat", deskripsiSingkat)
            }
            startActivity(intent)
        }


        buttonUpdateStok.setOnClickListener {
            val stokSekarang = textViewStok.text.toString().substringAfter(": ").toIntOrNull() ?: 0

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 30, 50, 10)
            }

            val textStokSaatIni = TextView(this).apply {
                text = "Stok saat ini: $stokSekarang"
                textSize = 16f
            }

            val editText = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                hint = "Masukkan jumlah stok baru"
            }

            container.addView(textStokSaatIni)
            container.addView(editText)

            AlertDialog.Builder(this)
                .setTitle("Update Jumlah Stok")
                .setView(container)
                .setPositiveButton("Simpan") { dialog, _ ->
                    val stokBaru = editText.text.toString().toIntOrNull()
                    if (stokBaru != null && stokBaru >= 0 && idMenu != null) {

                        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
                        progressDialog.progressHelper.barColor =
                            resources.getColor(R.color.maroon_700)
                        progressDialog.titleText = "Menyimpan perubahan..."
                        progressDialog.setCancelable(false)
                        progressDialog.show()

                        val db = FirebaseFirestore.getInstance()
                        db.collection("menus")
                            .document(idMenu!!)
                            .update(
                                mapOf(
                                    "stok" to stokBaru,
                                    "tersedia" to (stokBaru > 0)
                                )
                            )
                            .addOnSuccessListener {
                                progressDialog.dismissWithAnimation()
                                val successDialog =
                                    SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                                        .setTitleText("Stok berhasil diperbarui")
                                        .setContentText("Jumlah stok sekarang: $stokBaru")
                                successDialog.show()
                                successDialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility =
                                    android.view.View.GONE
                                Handler(Looper.getMainLooper()).postDelayed({
                                    successDialog.dismissWithAnimation()
                                }, 1500)
                            }
                            .addOnFailureListener {
                                progressDialog.dismissWithAnimation()
                                val errorDialog =
                                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Gagal memperbarui stok ❌")
                                        .setContentText("Coba cek koneksi atau ulangi lagi ya.")
                                errorDialog.show()
                                errorDialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility =
                                    android.view.View.GONE
                                Handler(Looper.getMainLooper()).postDelayed({
                                    errorDialog.dismissWithAnimation()
                                }, 2000)
                            }
                    } else {
                        val invalidDialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Input Tidak Valid")
                            .setContentText("Masukkan angka valid (0 atau lebih).")
                        invalidDialog.show()


                        invalidDialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility =
                            android.view.View.GONE


                        Handler(Looper.getMainLooper()).postDelayed({
                            invalidDialog.dismissWithAnimation()
                        }, 1500)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        listenerRegistration?.remove()

        idMenu?.let { id ->
            listenerRegistration = FirebaseFirestore.getInstance()
                .collection("menus")
                .document(id)
                .addSnapshotListener { doc, error ->
                    if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                    namaMenu = doc.getString("nama") ?: ""
                    hargaMenu = doc.getString("harga") ?: ""
                    val stokTerbaru = doc.getLong("stok")?.toInt() ?: 0
                    deskripsiMenu = doc.getString("deskripsi") ?: ""
                    deskripsiSingkat = doc.getString("deskripsiSingkat") ?: ""
                    imageUrl = doc.getString("gambar") ?: ""

                    textViewNamaMenu.text = namaMenu
                    textViewStok.text = "Stok: $stokTerbaru"
                    textViewDeskripsi.text = deskripsiMenu

                    val hargaInt = hargaMenu?.replace("[^\\d]".toRegex(), "")?.toIntOrNull() ?: 0
                    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                        maximumFractionDigits = 0
                    }
                    textViewHarga.text = "Harga: ${formatter.format(hargaInt)}"

                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_default_menu)
                        .error(R.drawable.ic_default_menu)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                        .into(findViewById(R.id.imageViewMenu))
                }
        }
    }

    override fun onPause() {
        super.onPause()
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
