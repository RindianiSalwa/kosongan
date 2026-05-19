package com.dapurandia.app.pembeli

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.dapurandia.app.R
import com.dapurandia.app.api.CloudinaryApiClient
import com.dapurandia.app.model.CloudinaryResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class EditProfilePembeliActivity : AppCompatActivity() {

    private lateinit var backIcon: ImageView
    private lateinit var editTextNama: EditText
    private lateinit var editTextNoHP: EditText
    private lateinit var buttonSimpan: Button
    private lateinit var imageProfile: ImageView
    private lateinit var buttonEditPhoto: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var selectedImageUri: Uri? = null
    private var uploadedPhotoUrl: String? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
        private const val UPLOAD_PRESET = "preset_profile"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pembeli_activity_edit_profile)

        backIcon = findViewById(R.id.backIcon)
        editTextNama = findViewById(R.id.editNama)
        editTextNoHP = findViewById(R.id.editNoHP)
        buttonSimpan = findViewById(R.id.buttonSimpan)
        imageProfile = findViewById(R.id.profileImage)
        buttonEditPhoto = findViewById(R.id.editPhotoIcon)

        backIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        loadProfileData()

        buttonEditPhoto.setOnClickListener {
            openImageChooser()
        }

        buttonSimpan.setOnClickListener {
            val namaBaru = editTextNama.text.toString().trim()
            val noHPBaru = editTextNoHP.text.toString().trim()

            if (namaBaru.isEmpty() || noHPBaru.isEmpty()) {
                Toast.makeText(this, "Isi semua data terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri != null) {
                uploadPhotoToCloudinary(selectedImageUri!!) { urlFotoBaru ->
                    saveProfileData(namaBaru, noHPBaru, urlFotoBaru)
                }
            } else {
                saveProfileData(namaBaru, noHPBaru, uploadedPhotoUrl)
            }
        }
    }

    private fun loadProfileData() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("pembeli").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    editTextNama.setText(doc.getString("nama") ?: "")
                    editTextNoHP.setText(doc.getString("no_hp") ?: "")

                    val photoUrl = doc.getString("fotoProfil")
                    uploadedPhotoUrl = photoUrl

                    android.util.Log.d("PROFILE_DEBUG", "Foto URL dari Firestore: $photoUrl")

                    Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(imageProfile)

                    android.util.Log.d("PROFILE_DEBUG", "Foto URL dari Firestore: $photoUrl")

                } else {
                    Toast.makeText(this, "Data pembeli tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data profil", Toast.LENGTH_SHORT).show()
            }
    }




    private fun openImageChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Pilih Foto Profil"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                Glide.with(this).load(selectedImageUri).into(imageProfile)
            }
        }
    }

    private fun uploadPhotoToCloudinary(imageUri: Uri, onSuccess: (String) -> Unit) {
        val file = uriToFile(imageUri)
        if (file == null) {
            Toast.makeText(this, "Gagal mengakses file gambar", Toast.LENGTH_SHORT).show()
            return
        }

        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val uploadPresetBody = RequestBody.create("text/plain".toMediaTypeOrNull(), UPLOAD_PRESET)

        CloudinaryApiClient.instance.uploadImage(multipartBody, uploadPresetBody)
            .enqueue(object : Callback<CloudinaryResponse> {
                override fun onResponse(call: Call<CloudinaryResponse>, response: Response<CloudinaryResponse>) {
                    if (response.isSuccessful) {
                        val urlFotoBaru = response.body()?.secureUrl
                        if (!urlFotoBaru.isNullOrEmpty()) {
                            onSuccess(urlFotoBaru)
                        } else {
                            Toast.makeText(this@EditProfilePembeliActivity, "Gagal mendapatkan URL foto", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@EditProfilePembeliActivity, "Upload gagal: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                    Toast.makeText(this@EditProfilePembeliActivity, "Upload gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveProfileData(nama: String, noHp: String, fotoProfilUrl: String?) {
        val userId = auth.currentUser?.uid ?: return
        val data: MutableMap<String, Any> = mutableMapOf(
            "nama" to nama,
            "no_hp" to noHp
        )
        if (!fotoProfilUrl.isNullOrEmpty()) {
            data["fotoProfil"] = fotoProfilUrl
        }

        val progressDialog = cn.pedant.SweetAlert.SweetAlertDialog(
            this,
            cn.pedant.SweetAlert.SweetAlertDialog.PROGRESS_TYPE
        )
        progressDialog.titleText = "Memperbarui profil..."
        progressDialog.progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
        progressDialog.setCancelable(false)
        progressDialog.show()

        firestore.collection("pembeli").document(userId)
            .update(data)
            .addOnSuccessListener {
                buttonSimpan.postDelayed({
                    progressDialog.changeAlertType(cn.pedant.SweetAlert.SweetAlertDialog.SUCCESS_TYPE)
                    progressDialog.titleText = "Berhasil!"
                    progressDialog.contentText = "Profil berhasil diperbarui."
                    progressDialog.setCancelable(false)
                    progressDialog.setConfirmClickListener(null)
                    progressDialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE

                    buttonSimpan.postDelayed({
                        progressDialog.dismissWithAnimation()
                        finish()
                    }, 1500)
                }, 1500)
            }
            .addOnFailureListener {
                buttonSimpan.postDelayed({
                    progressDialog.changeAlertType(cn.pedant.SweetAlert.SweetAlertDialog.ERROR_TYPE)
                    progressDialog.titleText = "Oops..."
                    progressDialog.contentText = "Gagal memperbarui profil, coba lagi."
                    progressDialog.setCancelable(true)
                }, 1500)
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
}
