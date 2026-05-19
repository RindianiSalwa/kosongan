package com.dapurandia.app.pembeli

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.dapurandia.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.util.Locale

class DetailMenuPembeliActivity : AppCompatActivity() {

    private lateinit var textViewStok: TextView
    private lateinit var buttonTambahKeranjang: Button
    private lateinit var textWarningStok: TextView
    private var listener: ListenerRegistration? = null
    private var idMenu: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pembeli_activity_detail_menu)

        val imageViewMenu = findViewById<ImageView>(R.id.imageViewMenu)
        val textViewNamaMenu = findViewById<TextView>(R.id.textViewNamaMenu)
        textViewStok = findViewById(R.id.textViewStok)
        val textViewHarga = findViewById<TextView>(R.id.textViewHarga)
        val textViewDeskripsi = findViewById<TextView>(R.id.textViewDeskripsi)
        buttonTambahKeranjang = findViewById(R.id.buttonTambahKeranjang)
        textWarningStok = findViewById(R.id.textWarningStok)
        val backIcon = findViewById<ImageView>(R.id.backIcon)

        idMenu = intent.getStringExtra("idMenu")

        backIcon.setOnClickListener { finish() }

        buttonTambahKeranjang.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val db = FirebaseFirestore.getInstance()
            val namaMenu = textViewNamaMenu.text.toString()
            val hargaMenuVal = textViewHarga.text.toString().replace("[^\\d]".toRegex(), "")
            val imageUrlVal = (imageViewMenu.tag as? String) ?: "" // simpan url di tag

            val keranjangRef = db.collection("keranjang").document(userId).collection("items")

            keranjangRef.whereEqualTo("namaMenu", namaMenu)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        val newItem = hashMapOf(
                            "namaMenu" to namaMenu,
                            "harga" to hargaMenuVal,
                            "imageUrl" to imageUrlVal,
                            "jumlah" to 1
                        )
                        keranjangRef.add(newItem)
                            .addOnSuccessListener {
                                val dialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                                dialog.titleText = "Berhasil ditambahkan ke keranjang"
                                dialog.setCancelable(false)
                                dialog.hideConfirmButton()
                                dialog.showCancelButton(false)
                                dialog.show()

                                Handler(Looper.getMainLooper()).postDelayed({
                                    dialog.dismissWithAnimation()
                                }, 1500)
                            }
                    } else {
                        val doc = documents.first()
                        val jumlahSaatIni = doc.getLong("jumlah") ?: 1
                        keranjangRef.document(doc.id)
                            .update("jumlah", jumlahSaatIni + 1)
                            .addOnSuccessListener {
                                val dialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                                dialog.titleText = "Jumlah diperbarui di keranjang"
                                dialog.setCancelable(false)
                                dialog.hideConfirmButton()
                                dialog.showCancelButton(false)
                                dialog.show()

                                Handler(Looper.getMainLooper()).postDelayed({
                                    dialog.dismissWithAnimation()
                                }, 1500)
                            }
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        idMenu?.let { id ->
            listener = FirebaseFirestore.getInstance()
                .collection("menus")
                .document(id)
                .addSnapshotListener { doc, error ->
                    if (error != null || doc == null || !doc.exists()) return@addSnapshotListener

                    val namaTerbaru = doc.getString("nama") ?: ""
                    val hargaTerbaru = doc.getString("harga") ?: ""
                    val stokTerbaru = doc.getLong("stok")?.toInt() ?: 0
                    val deskripsiTerbaru = doc.getString("deskripsi") ?: ""
                    val gambarTerbaru = doc.getString("gambar") ?: ""

                    findViewById<TextView>(R.id.textViewNamaMenu).text = namaTerbaru
                    findViewById<TextView>(R.id.textViewStok).text = "Stok: $stokTerbaru"
                    findViewById<TextView>(R.id.textViewDeskripsi).text = deskripsiTerbaru

                    val hargaInt = hargaTerbaru.replace("[^\\d]".toRegex(), "").toIntOrNull() ?: 0
                    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                        maximumFractionDigits = 0
                    }
                    findViewById<TextView>(R.id.textViewHarga).text = "Harga: ${formatter.format(hargaInt)}"

                    val imageView = findViewById<ImageView>(R.id.imageViewMenu)
                    val progressImage = findViewById<ProgressBar>(R.id.progressImageDetail)

                    progressImage.visibility = View.VISIBLE
                    Glide.with(this)
                        .load(gambarTerbaru)
                        .placeholder(R.drawable.ic_default_menu)
                        .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                            override fun onLoadFailed(
                                e: com.bumptech.glide.load.engine.GlideException?,
                                model: Any?,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                progressImage.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: android.graphics.drawable.Drawable,
                                model: Any,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                                dataSource: com.bumptech.glide.load.DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                progressImage.visibility = View.GONE
                                return false
                            }
                        })
                        .into(imageView)

                    imageView.tag = gambarTerbaru


                    updateButtonState(stokTerbaru)
                }
        }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    private fun updateButtonState(stok: Int) {
        if (stok <= 0) {
            buttonTambahKeranjang.isEnabled = false
            buttonTambahKeranjang.alpha = 0.5f
            textWarningStok.visibility = View.VISIBLE
        } else {
            buttonTambahKeranjang.isEnabled = true
            buttonTambahKeranjang.alpha = 1.0f
            textWarningStok.visibility = View.GONE
        }
    }
}
