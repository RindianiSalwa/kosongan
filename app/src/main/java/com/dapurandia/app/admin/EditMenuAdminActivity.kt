package com.dapurandia.app.admin

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.dapurandia.app.R
import com.dapurandia.app.api.CloudinaryApiClient
import com.dapurandia.app.model.CloudinaryResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import cn.pedant.SweetAlert.SweetAlertDialog

class EditMenuAdminActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var ivMenu: ImageView
    private lateinit var btnEditPhoto: ImageView
    private var idMenu: String? = null
    private lateinit var etNamaMenu: TextInputEditText
    private lateinit var etHargaMenu: TextInputEditText
    private lateinit var etDeskripsiSingkat: TextInputEditText
    private lateinit var etDeskripsiPanjang: TextInputEditText
    private lateinit var btnSimpan: Button

    private val firestore = FirebaseFirestore.getInstance()

    private var selectedImageUri: Uri? = null
    private var currentImageUrl: String? = null
    private var namaLama: String? = null

    companion object {
        private const val PICK_IMAGE_REQUEST_EDIT = 2002
        private const val UPLOAD_PRESET = "preset_menu"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_edit_menu)

        toolbar = findViewById(R.id.toolbarEditMenu)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
            title = "Edit Menu"
        }
        toolbar.setTitleTextColor(Color.WHITE)
        toolbar.navigationIcon?.setTint(Color.WHITE)
        toolbar.setNavigationOnClickListener { finish() }

        ivMenu = findViewById(R.id.ivMenu)
        btnEditPhoto = findViewById(R.id.btnEditPhoto)
        etNamaMenu = findViewById(R.id.etNamaMenu)
        etHargaMenu = findViewById(R.id.etHargaMenu)
        etDeskripsiSingkat = findViewById(R.id.etDeskripsiSingkat)
        etDeskripsiPanjang = findViewById(R.id.etDeskripsiPanjang)
        btnSimpan = findViewById(R.id.btnSimpanMenu)


        idMenu = intent.getStringExtra("idMenu")
        namaLama = intent.getStringExtra("namaMenu") ?: intent.getStringExtra("nama")
        val harga = intent.getStringExtra("hargaMenu") ?: intent.getStringExtra("harga") ?: ""
        val descSingkat = intent.getStringExtra("deskripsiSingkat") ?: ""
        val descPanjang = intent.getStringExtra("deskripsiPanjang") ?: intent.getStringExtra("deskripsi") ?: ""
        currentImageUrl = intent.getStringExtra("imageUrl") ?: intent.getStringExtra("gambar")

        // Prefill
        etNamaMenu.setText(namaLama)
        etHargaMenu.setText(harga)
        etDeskripsiSingkat.setText(descSingkat)
        etDeskripsiPanjang.setText(descPanjang)


        if (!currentImageUrl.isNullOrEmpty()) {
            Glide.with(this).load(currentImageUrl).placeholder(R.drawable.ic_default_menu).into(ivMenu)
        } else {
            ivMenu.setImageResource(R.drawable.ic_default_menu)
        }


        btnEditPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(intent, "Pilih Gambar Menu"), PICK_IMAGE_REQUEST_EDIT)
        }

        btnSimpan.setOnClickListener {
            val namaBaru = etNamaMenu.text?.toString()?.trim().orEmpty()
            val hargaBaruRaw = etHargaMenu.text?.toString()?.trim().orEmpty()
            val descSingkatBaru = etDeskripsiSingkat.text?.toString()?.trim().orEmpty()
            val descPanjangBaru = etDeskripsiPanjang.text?.toString()?.trim().orEmpty()

            //Bersihkan harga
            val hargaBaru = hargaBaruRaw.replace(".", "").replace(",", "")

            if (namaBaru.isEmpty() || hargaBaru.isEmpty() ||
                descSingkatBaru.isEmpty() || descPanjangBaru.isEmpty() ||
                (selectedImageUri == null && currentImageUrl.isNullOrEmpty())
            ) {
                val dialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Semua kolom wajib diisi!")
                dialog.show()

                dialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE

                Handler(Looper.getMainLooper()).postDelayed({
                    if (dialog.isShowing) dialog.dismissWithAnimation()
                }, 2000)

                return@setOnClickListener
            }

            if (idMenu == null) {
                val dialog = SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("ID menu tidak ditemukan")
                dialog.show()


                dialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE
                Handler(Looper.getMainLooper()).postDelayed({
                    if (dialog.isShowing) dialog.dismissWithAnimation()
                }, 2000)

                return@setOnClickListener
            }

            if (selectedImageUri != null) {
                uploadImageToCloudinary(selectedImageUri!!) { newUrl ->
                    updateMenuById(
                        idMenu!!,
                        namaBaru,
                        hargaBaru,
                        descSingkatBaru,
                        descPanjangBaru,
                        newUrl
                    )
                }
            } else {
                updateMenuById(
                    idMenu!!,
                    namaBaru,
                    hargaBaru,
                    descSingkatBaru,
                    descPanjangBaru,
                    null
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST_EDIT && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { ivMenu.setImageURI(it) }
        }
    }

    private fun uploadImageToCloudinary(uri: Uri, onSuccess: (String) -> Unit) {
        val file = uriToFile(uri)
        if (file == null) {
            Toast.makeText(this, "Gagal akses file gambar", Toast.LENGTH_SHORT).show()
            return
        }

        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val uploadPresetBody = RequestBody.create("text/plain".toMediaTypeOrNull(), UPLOAD_PRESET)

        CloudinaryApiClient.instance.uploadImage(multipartBody, uploadPresetBody)
            .enqueue(object : Callback<CloudinaryResponse> {
                override fun onResponse(call: Call<CloudinaryResponse>, response: Response<CloudinaryResponse>) {
                    if (response.isSuccessful) {
                        val imageUrl = response.body()?.secureUrl
                        if (!imageUrl.isNullOrEmpty()) {
                            onSuccess(imageUrl)
                        } else {
                            Toast.makeText(this@EditMenuAdminActivity, "Gagal mendapatkan URL gambar", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@EditMenuAdminActivity, "Upload gagal: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                    Toast.makeText(this@EditMenuAdminActivity, "Upload gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload", ".jpg", cacheDir)
            tempFile.outputStream().use { output ->
                inputStream?.copyTo(output)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateMenuById(
        idMenu: String,
        nama: String,
        harga: String,
        deskripsiSingkat: String,
        deskripsiPanjang: String,
        newImageUrlOrNull: String?
    ) {
        val updateMap = mutableMapOf<String, Any>(
            "nama" to nama,
            "harga" to harga,
            "deskripsiSingkat" to deskripsiSingkat,
            "deskripsi" to deskripsiPanjang
        )
        if (!newImageUrlOrNull.isNullOrEmpty()) {
            updateMap["gambar"] = newImageUrlOrNull
        }

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.progressHelper.barColor = resources.getColor(R.color.maroon_700)
        progressDialog.titleText = "Menyimpan perubahan..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        firestore.collection("menus").document(idMenu)
            .update(updateMap)
            .addOnSuccessListener {
                progressDialog.dismissWithAnimation()
                val successDialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Menu berhasil diperbarui")
                    .setContentText("Data menu sudah disimpan")
                successDialog.show()

                successDialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE

                Handler(Looper.getMainLooper()).postDelayed({
                    successDialog.dismissWithAnimation()
                    finish()
                }, 1500)
            }
            .addOnFailureListener { e ->
                progressDialog.dismissWithAnimation()
                val errorDialog = SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Gagal update ❌")
                    .setContentText("Coba cek koneksi atau ulangi lagi ya.")
                errorDialog.show()

                errorDialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE

                Handler(Looper.getMainLooper()).postDelayed({
                    errorDialog.dismissWithAnimation()
                }, 2000)
            }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            currentFocus?.let { view ->
                if (view is android.widget.EditText) {
                    val outRect = android.graphics.Rect()
                    view.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        view.clearFocus()
                        val imm =
                            getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.hideSoftInputFromWindow(view.windowToken, 0)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

}
