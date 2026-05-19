package com.dapurandia.app.pembeli

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dapurandia.app.BuildConfig
import com.dapurandia.app.R
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import android.view.inputmethod.InputMethodManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.random.Random

class RegisterPembeliActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val fonnteClient = OkHttpClient()

    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private var pendingRegistration: PendingRegistration? = null
    private var pendingOtp: String? = null
    private var otpCreatedAt: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pembeli_activity_register)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.rootLayout)

        rootLayout.setOnClickListener {
            hideKeyboard()
        }


        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        nameEditText = findViewById(R.id.namaEditText)
        phoneEditText = findViewById(R.id.noHpEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showAutoDismissDialog(
                    SweetAlertDialog.WARNING_TYPE,
                    "Peringatan",
                    "Semua kolom harus diisi!"
                )
                return@setOnClickListener
            }

            val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
            progressDialog.titleText = "Mengirim OTP..."
            progressDialog.setCancelable(false)
            progressDialog.show()

            requestOtp(PendingRegistration(name, phone, email, password), progressDialog)
        }
    }

    private fun requestOtp(registration: PendingRegistration, progressDialog: SweetAlertDialog) {
        val token = BuildConfig.FONNTE_TOKEN.trim()
        if (token.isEmpty()) {
            progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
            progressDialog.titleText = "Token Fonnte Belum Ada"
            progressDialog.contentText = "Tambahkan fonnte.token di local.properties."
            hideConfirmAndAutoDismiss(progressDialog, false)
            return
        }

        val otp = Random.nextInt(100000, 999999).toString()
        val normalizedPhone = normalizePhoneNumber(registration.phone)
        val message = "Kode OTP Dapur Andia Anda: $otp. Jangan bagikan kode ini kepada siapa pun."
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("target", normalizedPhone)
            .addFormDataPart("message", message)
            .addFormDataPart("countryCode", "62")
            .build()
        val request = Request.Builder()
            .url("https://api.fonnte.com/send")
            .addHeader("Authorization", token)
            .post(body)
            .build()

        fonnteClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                    progressDialog.titleText = "OTP Gagal Dikirim"
                    progressDialog.contentText = "Periksa koneksi internet Anda."
                    hideConfirmAndAutoDismiss(progressDialog, false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(responseBody) }.getOrNull()
                val isSent = response.isSuccessful && json?.optBoolean("status", false) == true
                val detail = json?.optString("detail").orEmpty()

                runOnUiThread {
                    if (isSent) {
                        pendingRegistration = registration.copy(phone = normalizedPhone)
                        pendingOtp = otp
                        otpCreatedAt = System.currentTimeMillis()
                        progressDialog.dismissWithAnimation()
                        showOtpDialog()
                    } else {
                        progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                        progressDialog.titleText = "OTP Gagal Dikirim"
                        progressDialog.contentText = detail.ifEmpty { "Fonnte tidak menerima permintaan OTP." }
                        hideConfirmAndAutoDismiss(progressDialog, false)
                    }
                }
            }
        })
    }

    private fun showOtpDialog() {
        val otpInput = EditText(this).apply {
            hint = "Masukkan kode OTP"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        val container = FrameLayout(this).apply {
            val padding = resources.getDimensionPixelSize(R.dimen.dialog_padding)
            setPadding(padding, 0, padding, 0)
            addView(otpInput)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Verifikasi OTP")
            .setMessage("Kode OTP sudah dikirim ke WhatsApp. Masukkan kode untuk menyelesaikan pendaftaran.")
            .setView(container)
            .setPositiveButton("Verifikasi", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val otp = pendingOtp
                val registration = pendingRegistration
                val inputOtp = otpInput.text.toString().trim()
                val isExpired = System.currentTimeMillis() - otpCreatedAt > OTP_VALID_DURATION_MS

                when {
                    otp == null || registration == null || isExpired -> {
                        dialog.dismiss()
                        showAutoDismissDialog(
                            SweetAlertDialog.WARNING_TYPE,
                            "OTP Kedaluwarsa",
                            "Silakan tekan Buat Akun untuk meminta OTP baru."
                        )
                    }
                    inputOtp != otp -> {
                        otpInput.error = "Kode OTP tidak sesuai"
                    }
                    else -> {
                        dialog.dismiss()
                        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
                        progressDialog.titleText = "Sedang mendaftar..."
                        progressDialog.setCancelable(false)
                        progressDialog.show()
                        createPembeliAccount(registration, progressDialog)
                    }
                }
            }
        }
        dialog.show()
    }

    private fun createPembeliAccount(
        registration: PendingRegistration,
        progressDialog: SweetAlertDialog
    ) {
        auth.createUserWithEmailAndPassword(registration.email, registration.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    val pembeliData = hashMapOf(
                        "nama" to registration.name,
                        "no_hp" to registration.phone,
                        "email" to registration.email,
                        "role" to "pembeli"
                    )

                    if (uid != null) {
                        firestore.collection("pembeli").document(uid)
                            .set(pembeliData)
                            .addOnSuccessListener {
                                clearPendingOtp()
                                progressDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                                progressDialog.titleText = "Berhasil!"
                                progressDialog.contentText = "Registrasi berhasil dilakukan."
                                hideConfirmAndAutoDismiss(progressDialog, true)
                            }
                            .addOnFailureListener {
                                progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                                progressDialog.titleText = "Oops..."
                                progressDialog.contentText = "Gagal menyimpan data: ${it.message}"
                                hideConfirmAndAutoDismiss(progressDialog, false)
                            }
                    }
                } else {
                    progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                    progressDialog.titleText = "Registrasi Gagal"
                    progressDialog.contentText = getRegisterErrorMessage(task.exception)
                    hideConfirmAndAutoDismiss(progressDialog, false)
                }
            }
    }

    private fun normalizePhoneNumber(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("62") -> digits
            digits.startsWith("0") -> digits
            digits.startsWith("8") -> "62$digits"
            else -> digits
        }
    }

    private fun clearPendingOtp() {
        pendingRegistration = null
        pendingOtp = null
        otpCreatedAt = 0L
    }

    private fun showAutoDismissDialog(type: Int, title: String, message: String) {
        val dialog = SweetAlertDialog(this, type)
            .setTitleText(title)
            .setContentText(message)
        dialog.show()

        dialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismissWithAnimation()
        }, 2000)
    }

    private fun hideConfirmAndAutoDismiss(dialog: SweetAlertDialog, finishAfter: Boolean) {
        dialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismissWithAnimation()
            if (finishAfter) finish()
        }, 2000)
    }
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }

    private fun getRegisterErrorMessage(e: Exception?): String {
        return when (e) {
            is FirebaseAuthWeakPasswordException ->
                "Password terlalu lemah, gunakan minimal 6 karakter."
            is FirebaseAuthUserCollisionException ->
                "Email sudah digunakan, silakan gunakan email lain."
            is FirebaseNetworkException ->
                "Tidak ada koneksi internet. Silakan periksa jaringan Anda."
            else ->
                "Registrasi gagal, silakan coba lagi."
        }
    }

    private data class PendingRegistration(
        val name: String,
        val phone: String,
        val email: String,
        val password: String
    )

    private companion object {
        const val OTP_VALID_DURATION_MS = 5 * 60 * 1000L
    }
}
