package com.dapurandia.app.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.dapurandia.app.R
import com.dapurandia.app.api.CloudinaryApiClient
import com.dapurandia.app.model.CloudinaryResponse
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import android.graphics.Color
import cn.pedant.SweetAlert.SweetAlertDialog
import java.util.UUID
import android.view.inputmethod.InputMethodManager

class TambahMenuAdminActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var imageViewMenu: ImageView
    private lateinit var buttonPickImage: ImageButton
    private lateinit var editTextNamaMenu: EditText
    private lateinit var editTextHargaMenu: EditText
    private lateinit var editTextStokMenu: EditText
    private lateinit var editTextDeskripsiSingkat: EditText
    private lateinit var editTextDeskripsiMenu: EditText
    private lateinit var buttonSimpanMenu: Button

    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var loadingDialog: SweetAlertDialog

    companion object {
        private const val PICK_IMAGE_REQUEST = 1002
        private const val UPLOAD_PRESET = "preset_menu"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_tambah_menu)


        toolbar = findViewById(R.id.toolbarTambahMenu)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
            title = "Tambah Menu"
        }
        toolbar.setTitleTextColor(Color.WHITE)
        toolbar.navigationIcon?.setTint(Color.WHITE)

        imageViewMenu = findViewById(R.id.imageViewMenu)
        buttonPickImage = findViewById(R.id.buttonPickImage)
        editTextNamaMenu = findViewById(R.id.editTextNamaMenu)
        editTextHargaMenu = findViewById(R.id.editTextHargaMenu)
        editTextStokMenu = findViewById(R.id.editTextStokMenu)
        editTextDeskripsiSingkat = findViewById(R.id.editTextDeskripsiSingkat)
        editTextDeskripsiMenu = findViewById(R.id.editTextDeskripsiMenu)
        buttonSimpanMenu = findViewById(R.id.buttonSimpanMenu)

        // init loading dialog
        loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.progressHelper.barColor = resources.getColor(R.color.maroon_700)
        loadingDialog.titleText = "Mohon tunggu..."
        loadingDialog.setCancelable(false)

        buttonPickImage.setOnClickListener { openImageChooser() }

        buttonSimpanMenu.setOnClickListener {
            val nama = editTextNamaMenu.text.toString().trim()
            val harga = editTextHargaMenu.text.toString().trim()
            val stok = editTextStokMenu.text.toString().trim()
            val hargaMurni = harga.replace(".", "").replace(",", "")
            val deskripsiSingkat = editTextDeskripsiSingkat.text.toString().trim()
            val deskripsi = editTextDeskripsiMenu.text.toString().trim()

            if (nama.isEmpty() || harga.isEmpty() || stok.isEmpty() || deskripsi.isEmpty() || deskripsiSingkat.isEmpty()) {
                showAutoDismissDialog("Oops!", "Semua data harus diisi!", SweetAlertDialog.ERROR_TYPE)
                return@setOnClickListener
            }

            if (selectedImageUri != null) {
                loadingDialog.show()
                uploadImageToCloudinary(selectedImageUri!!) { imageUrl ->
                    uploadedImageUrl = imageUrl
                    saveMenuToFirestore(nama, hargaMurni, stok.toInt(), deskripsiSingkat, deskripsi, imageUrl)
                }
            } else {
                showAutoDismissDialog("Oops!", "Pilih gambar menu terlebih dahulu", SweetAlertDialog.ERROR_TYPE)
            }
        }
    }

    private fun openImageChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Pilih Gambar Menu"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { imageViewMenu.setImageURI(it) }
        }
    }

    private fun uploadImageToCloudinary(uri: Uri, onSuccess: (String) -> Unit) {
        val file = uriToFile(uri)
        if (file == null) {
            loadingDialog.dismiss()
            showAutoDismissDialog("Oops!", "Gagal mengakses file gambar", SweetAlertDialog.ERROR_TYPE)
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
                            loadingDialog.dismiss()
                            showAutoDismissDialog("Oops!", "Upload gagal, URL kosong", SweetAlertDialog.ERROR_TYPE)
                        }
                    } else {
                        loadingDialog.dismiss()
                        showAutoDismissDialog("Oops!", "Upload gagal, coba lagi nanti", SweetAlertDialog.ERROR_TYPE)
                    }
                }

                override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                    loadingDialog.dismiss()
                    showAutoDismissDialog("Oops!", "Upload gagal, periksa koneksi internet", SweetAlertDialog.ERROR_TYPE)
                }
            })
    }

    private fun saveMenuToFirestore(
        nama: String,
        harga: String,
        stok: Int,
        deskripsiSingkat: String,
        deskripsiPanjang: String,
        imageUrl: String
    ) {
        val idMenu = UUID.randomUUID().toString()
        val dataMenu = hashMapOf(
            "idMenu" to idMenu,
            "nama" to nama,
            "harga" to harga,
            "stok" to stok,
            "deskripsi" to deskripsiPanjang,
            "deskripsiSingkat" to deskripsiSingkat,
            "gambar" to imageUrl
        )

        firestore.collection("menus")
            .document(idMenu)
            .set(dataMenu)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                showAutoDismissDialog("Berhasil", "Menu berhasil ditambahkan!", SweetAlertDialog.SUCCESS_TYPE)

                val resultIntent = Intent().apply {
                    putExtra("idMenu", idMenu)
                    putExtra("namaMenu", nama)
                    putExtra("hargaMenu", harga)
                    putExtra("stokMenu", stok)
                    putExtra("deskripsiMenu", deskripsiSingkat)
                    putExtra("deskripsiPanjang", deskripsiPanjang)
                    putExtra("imageUrl", imageUrl)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener {
                loadingDialog.dismiss()
                showAutoDismissDialog("Oops!", "Gagal menambahkan menu", SweetAlertDialog.ERROR_TYPE)
            }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload", ".jpg", cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    private fun showAutoDismissDialog(title: String, message: String, type: Int) {
        val dialog = SweetAlertDialog(this, type)
            .setTitleText(title)
            .setContentText(message)

        dialog.show()
        dialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = Button.GONE

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismissWithAnimation()
        }, 2000)
    }
    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            currentFocus?.let { view ->
                if (view is EditText) {
                    val outRect = android.graphics.Rect()
                    view.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        view.clearFocus()
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(view.windowToken, 0)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

}
